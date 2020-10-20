package com.example.repeatingalarmfoss.screens.main

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.repeatingalarmfoss.*
import com.example.repeatingalarmfoss.db.RepeatingClassifier
import com.example.repeatingalarmfoss.helper.DEFAULT_UI_SKIP_DURATION
import com.example.repeatingalarmfoss.helper.FixedSizeBitSet
import com.example.repeatingalarmfoss.helper.extensions.inflate
import com.example.repeatingalarmfoss.helper.extensions.set
import com.example.repeatingalarmfoss.helper.extensions.toast
import com.example.repeatingalarmfoss.screens.AlarmActivity
import com.example.repeatingalarmfoss.screens.TimePickerFragment
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function7
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream

class MainActivity : AppCompatActivity(), TimePickerFragment.OnTimeSetCallback {
    private val clicks = CompositeDisposable()
    private val tasksViewModel: TasksViewModel by viewModels()
    private val tasksAdapter = TasksAdapter(::removeTask)
    private lateinit var timeSetInDialogTextView: TextView

    override fun onDestroy() = super.onDestroy().also { clicks.clear() }

    private fun removeTask(id: Long) = tasksViewModel.removeTask(id)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupClicks()
        setupViewModelSubscriptions()
        setupTaskList()
    }

    private fun setupTaskList() {
        tasksList.layoutManager = LinearLayoutManager(this)
        tasksList.adapter = tasksAdapter
        tasksViewModel.fetchTasks()
    }

    private fun setupClicks() {
        clicks += addTaskFab.clicks()
            .throttleFirst(DEFAULT_UI_SKIP_DURATION, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribe { showAddTaskDialog() }
    }

    private fun setupViewModelSubscriptions() {
        tasksViewModel.addTaskEvent.observe(this, Observer { task ->
            scheduleAlarmManager(task.description, task.repeatingClassifierValue, task.time)
            tasksAdapter.addNewTask(task)
        })
        tasksViewModel.removeTaskEvent.observe(this, Observer { id ->
            tasksAdapter.removeTask(id)
        })
        tasksViewModel.fetchAllTasksEvent.observe(this, Observer {
            tasksAdapter.tasks = it.toMutableList()
        })
        tasksViewModel.errorEvent.observe(this, Observer { errorMessage ->
            toast(getString(errorMessage))
        })
    }

    private fun scheduleAlarmManager(title: String, repeatingClassifierValue: String, time: String) {
        val nextLaunchTime = tasksViewModel.getNextLaunchTime(time, repeatingClassifierValue)
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ACTION_RING
            putExtra(ALARM_ARG_TITLE, title)
            putExtra(ALARM_ARG_INTERVAL, repeatingClassifierValue)
            putExtra(ALARM_ARG_TIME, time)
        }
        Log.d("ABC", "abc first launch: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.UK).format(nextLaunchTime)}")

        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(nextLaunchTime, PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun showAddTaskDialog() {
        val chosenWeekDays = FixedSizeBitSet(7)
        val dialogView = (root as ViewGroup).inflate(R.layout.dialog_creating_task)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.etTaskDescription)
        timeSetInDialogTextView = dialogView.findViewById(R.id.timeSetValueTextView)

        clicks += dialogView.findViewById<Button>(R.id.notificationTimeSetButton).clicks()
            .throttleFirst(DEFAULT_UI_SKIP_DURATION, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribe { TimePickerFragment(this).show(supportFragmentManager, TimePickerFragment::class.java.simpleName) }

        clicks += Observable.combineLatest(
            dialogView.findViewById<ToggleButton>(R.id.toggleMon).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleTue).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleWed).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleThu).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleFri).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleSat).checkedChanges(),
            dialogView.findViewById<ToggleButton>(R.id.toggleSun).checkedChanges(),
            Function7<Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Unit> { mon, tue, wed, thu, fri, sat, sun ->
                chosenWeekDays.apply {/*todo to viewmodel\usecase, test*/
                    if (mon) set(0) else clear(0)
                    if (tue) set(1) else clear(1)
                    if (wed) set(2) else clear(2)
                    if (thu) set(3) else clear(3)
                    if (fri) set(4) else clear(4)
                    if (sat) set(5) else clear(5)
                    if (sun) set(6) else clear(6)
                }
            }).subscribe()

        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_new_task))
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss().also {
                    val description = descriptionEditText.text.toString()
                    val repeatingClassifier: RepeatingClassifier = RepeatingClassifier.DAY_OF_WEEK /*fixme*/
                    val time = timeSetInDialogTextView.text.toString()
                    tasksViewModel.addTask(description, repeatingClassifier, chosenWeekDays.toString(), time)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .apply { show() }

        clicks += descriptionEditText.textChanges()
            .map { it.isBlank().not() }
            .subscribe { dialog.getButton(BUTTON_POSITIVE).isEnabled = it }
    }

    @SuppressLint("SetTextI18n")
    override fun onTimeSet(hourOfDay: Int, minutes: Int) {
        timeSetInDialogTextView.text = "$hourOfDay:$minutes"
    }
}