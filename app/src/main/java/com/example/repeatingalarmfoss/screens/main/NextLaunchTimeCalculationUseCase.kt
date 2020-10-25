package com.example.repeatingalarmfoss.screens.main

import com.example.repeatingalarmfoss.helper.FixedSizeBitSet
import com.example.repeatingalarmfoss.helper.extensions.LongExt.daysToMilliseconds
import com.example.repeatingalarmfoss.helper.extensions.LongExt.hoursToMilliseconds
import com.example.repeatingalarmfoss.helper.extensions.LongExt.minutesToMilliseconds
import com.example.repeatingalarmfoss.helper.extensions.LongExt.secondsToMilliseconds
import java.util.*

class NextLaunchTimeCalculationUseCase {
    /** @param time - Timestamp, implies hours (in 24-hour format) and minutes divided with separator ":". For example, 21:12

     *  @param chosenWeekDaysBinaryString - String, denoting weekdays "chosen" or not in binary format, first index is Sunday, second is Monday, etc.
    For example, argument value is: 0100001. This means, "chosen" days are Monday and Saturday

     *  @return - Timestamp in format of milliseconds, denoting time in future.
     * */
    fun getNexLaunchTime(time: String, chosenWeekDaysBinaryString: String): Long {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val chosenWeekDays = FixedSizeBitSet.fromBinaryString(chosenWeekDaysBinaryString)
        val hours = time.split(":")[0].toInt()
        val minutes = time.split(":")[1].toInt()
        val timeIsLeft = hours <= Calendar.getInstance().get(Calendar.HOUR_OF_DAY) && minutes <= Calendar.getInstance().get(Calendar.MINUTE)
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, if (chosenWeekDays.get(today) && timeIsLeft) today + 1 else today)
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getNextLaunchTime(currentTime: Long, interval: Int, classifier: String): Long = when (classifier) {
        "second" -> currentTime + secondsToMilliseconds(interval.toLong())
        "minute" -> currentTime + minutesToMilliseconds(interval.toLong())
        "hour" -> currentTime + hoursToMilliseconds(interval.toLong())
        "day" -> currentTime + daysToMilliseconds(interval.toLong())
        else -> throw IllegalArgumentException()
    }
}