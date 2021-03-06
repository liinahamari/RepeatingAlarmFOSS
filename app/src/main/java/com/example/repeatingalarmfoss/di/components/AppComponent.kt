package com.example.repeatingalarmfoss.di.components

import com.example.repeatingalarmfoss.RepeatingAlarmApp
import com.example.repeatingalarmfoss.base.*
import com.example.repeatingalarmfoss.di.ViewModelBuilderModule
import com.example.repeatingalarmfoss.di.modules.*
import com.example.repeatingalarmfoss.receivers.LowBatteryTracker
import com.example.repeatingalarmfoss.screens.added_tasks.MainActivity
import com.example.repeatingalarmfoss.screens.added_tasks.SetupAddingTaskFragment
import com.example.repeatingalarmfoss.screens.added_tasks.TaskListFragment
import com.example.repeatingalarmfoss.screens.alarm.AlarmActivity
import com.example.repeatingalarmfoss.screens.logs.LogsActivity
import com.example.repeatingalarmfoss.services.AlarmNotifierService
import com.example.repeatingalarmfoss.services.NextLaunchPreparingService
import com.example.repeatingalarmfoss.services.ReschedulingAlarmsOnBootService
import com.example.repeatingalarmfoss.widget.WidgetProvider
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DatabaseModule::class, ContextModule::class, LoggerModule::class, SchedulerModule::class, ServiceModule::class, ViewModelBuilderModule::class, AddTaskModule::class, MainActivityModule::class])
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: RepeatingAlarmApp): Builder
        fun build(): AppComponent
    }

    fun biometricComponent(module: BiometricModule): BiometricComponent

    fun inject(activity: AlarmActivity)
    fun inject(activity: NotifyingActivity)
    fun inject(activity: BaseActivity)
    fun inject(activity: TaskListFragment)
    fun inject(service: NextLaunchPreparingService)
    fun inject(service: BaseService)
    fun inject(service: BaseJobIntentService)
    fun inject(service: ReschedulingAlarmsOnBootService)
    fun inject(service: ForegroundService)
    fun inject(service: AlarmNotifierService)
    fun inject(widgetProvider: WidgetProvider)
    fun inject(app: RepeatingAlarmApp)
    fun inject(receiver: LowBatteryTracker)
    fun inject(fragment: SetupAddingTaskFragment)
    fun inject(activity: LogsActivity)
}
