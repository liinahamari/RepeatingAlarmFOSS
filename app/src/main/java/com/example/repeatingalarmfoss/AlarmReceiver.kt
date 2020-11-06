package com.example.repeatingalarmfoss

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.repeatingalarmfoss.db.RepeatingClassifier
import com.example.repeatingalarmfoss.helper.FlightRecorder
import com.example.repeatingalarmfoss.screens.alarm.AlarmActivity
import com.example.repeatingalarmfoss.screens.NotifierService
import java.util.*
import com.example.repeatingalarmfoss.helper.extensions.set
import com.example.repeatingalarmfoss.usecases.NextLaunchTimeCalculationUseCase
import java.text.SimpleDateFormat
import javax.inject.Inject

const val ACTION_RING = "action_ring"
const val ALARM_ARG_INTERVAL = "arg_interval"
const val ALARM_ARG_TIME = "arg_time"
const val ALARM_ARG_TITLE = "arg_title"
const val ALARM_ARG_CLASSIFIER = "arg_classifier"

class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var logger: FlightRecorder
    private val nextLaunchTimeCalculationUseCase = NextLaunchTimeCalculationUseCase()

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as RepeatingAlarmApp).appComponent.inject(this)

        val title = intent.getStringExtra(ALARM_ARG_TITLE)
        if (Build.VERSION.SDK_INT >= 29 && RepeatingAlarmApp.INSTANCE.isAppInForeground.not()) {
            ContextCompat.startForegroundService(context, Intent(context, NotifierService::class.java).apply { putExtra(NotifierService.ARG_TASK_TITLE, title) })
        } else {
            context.startActivity(Intent(context, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(NotifierService.ARG_TASK_TITLE, title)
            })
        }

        if (intent.action == ACTION_RING) {
            val time = intent.getStringExtra(ALARM_ARG_TIME)!!
            val repeatingClassifier = intent.getStringExtra(ALARM_ARG_CLASSIFIER)!!
            val repeatingClassifierValue = intent.getStringExtra(ALARM_ARG_INTERVAL)!!
            val nextLaunchTime: Long = if (repeatingClassifier == RepeatingClassifier.EVERY_X_TIME_UNIT.name) {
                nextLaunchTimeCalculationUseCase.getNextLaunchTime(time.toLong(), repeatingClassifierValue)
            } else {
                nextLaunchTimeCalculationUseCase.getNextLaunchTime(time, repeatingClassifierValue)
            }
            val newIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_RING
                putExtra(ALARM_ARG_INTERVAL, intent.getStringExtra(ALARM_ARG_INTERVAL))
                putExtra(ALARM_ARG_TITLE, title)
                putExtra(ALARM_ARG_CLASSIFIER, repeatingClassifier)
                putExtra(ALARM_ARG_TIME, nextLaunchTime.toString())
            }
            logger.d(true) { "Next launch: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.UK).format(nextLaunchTime)}" }
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.set(nextLaunchTime, PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        }
    }
}