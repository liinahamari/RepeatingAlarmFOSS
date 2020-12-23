@file:Suppress("RedundantSamConstructor")

package com.example.repeatingalarmfoss.screens.added_tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.repeatingalarmfoss.R
import com.example.repeatingalarmfoss.RepeatingAlarmApp
import com.example.repeatingalarmfoss.base.BaseFragment
import com.example.repeatingalarmfoss.db.Task
import com.example.repeatingalarmfoss.helper.FlightRecorder
import com.example.repeatingalarmfoss.helper.extensions.set
import com.example.repeatingalarmfoss.helper.extensions.throttleFirst
import com.example.repeatingalarmfoss.helper.extensions.toast
import com.example.repeatingalarmfoss.receivers.AlarmReceiver
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_task_list.*
import javax.inject.Inject

class TaskListFragment : BaseFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val addingTasksViewModel by activityViewModels<AddingTasksViewModel> { viewModelFactory }

    @Inject
    lateinit var logger: FlightRecorder

    @Inject
    lateinit var alarmManager: AlarmManager
    private var onTaskAddedCallback: TaskAddedCallback? = null

    companion object {
        fun newInstance() = TaskListFragment()
    }

    override fun onAttach(context: Context) = super.onAttach(context).also { onTaskAddedCallback = context as MainActivity } /*todo mb shared viewModel?*/
    override fun onDetach() = super.onDetach().also { onTaskAddedCallback = null }

    private val tasksAdapter = AddedTasksAdapter(::removeTask)
    private fun removeTask(id: Long) = addingTasksViewModel.removeTask(id)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_task_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity().application as RepeatingAlarmApp).appComponent.apply {
            inject(this@TaskListFragment)
        }

        if (requireActivity().pager == null) {
            addTaskFab.isVisible = false
        } else {
            setupClicks()
        }
        setupViewModelSubscriptions()
        setupTaskList()
    }

    private fun setupTaskList() {
        tasksList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }
    }

    override fun onResume() = super.onResume().also { addingTasksViewModel.fetchTasks() }

    private fun setupClicks() {
        clicks += addTaskFab.clicks()
            .throttleFirst()
            .subscribe { SetupAddingTaskFragment.newInstance().show(childFragmentManager, SetupAddingTaskFragment::class.java.simpleName) }
    }

    private fun setupViewModelSubscriptions() {
        addingTasksViewModel.addTaskEvent.observe(viewLifecycleOwner, Observer { task ->
            tasksAdapter.addNewTask(task)
            onTaskAddedCallback?.onSuccessfulScheduling()
        })
        addingTasksViewModel.removeTaskEvent.observe(viewLifecycleOwner, Observer { id ->
            cancelAlarmManagerFor(id)
            tasksAdapter.removeTask(id)
        })
        addingTasksViewModel.fetchAllTasksEvent.observe(viewLifecycleOwner, Observer {
            tasksAdapter.tasks = it.toMutableList()
        })
        addingTasksViewModel.scheduleTaskEvent.observe(viewLifecycleOwner, Observer {
            scheduleAlarmManager(it)
        })
        addingTasksViewModel.errorEvent.observe(viewLifecycleOwner, Observer { errorMessage ->
            toast(getString(errorMessage))
        })
    }

    private fun cancelAlarmManagerFor(id: Long) = alarmManager.cancel(PendingIntent.getBroadcast(requireContext(), id.toInt(), Intent(requireActivity(), AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

    private fun scheduleAlarmManager(task: Task) {
        val intent = AlarmReceiver.createIntent(task, requireActivity())
        logger.logScheduledEvent(what = { "First launch:" }, `when` = task.time.toLong())
        alarmManager.set(task.time.toLong(), PendingIntent.getBroadcast(requireContext(), task.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT))
    }
}

interface TaskAddedCallback {
    fun onSuccessfulScheduling()
}