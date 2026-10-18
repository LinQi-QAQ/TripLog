package com.linqi.triplog.remind

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.linqi.triplog.data.local.Trip
import java.time.Instant
import java.time.ZoneId

/**
 * 出发提醒调度。用 AlarmManager 的 setExactAndAllowWhileIdle，
 * 开机后由 BootReceiver 重建。
 */
object ReminderScheduler {

    const val EXTRA_TRIP_ID = "trip_id"
    const val EXTRA_TRIP_TITLE = "trip_title"

    fun schedule(context: Context, trip: Trip) {
        val depart = trip.departTime ?: return
        val before = trip.remindBeforeMinutes
        if (before <= 0) return
        val triggerAt = depart - before * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, trip.id)

        // OnAlarmListener 形式只在 API 33 之后有，先只用三参数版本，Intent 里带数据
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(context: Context, tripId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, tripId))
    }

    fun rescheduleAll(context: Context, trips: List<Trip>) {
        trips.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, tripId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TRIP_ID, tripId)
            putExtra(EXTRA_TRIP_TITLE, titleFallback(tripId))
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, tripId.toInt(), intent, flags)
    }
}

fun Trip.title(): String {
    val num = if (number.isBlank()) "" else " $number"
    return "$fromStation 到 $toStation$num".trim()
}

/** 调度时还不知道标题，先用占位；Receiver 里会查真实 Trip 覆盖。 */
private fun titleFallback(tripId: Long): String = "行程提醒"

fun Trip.departLabel(): String {
    val t = departTime ?: return "未定时间"
    val dt = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "%d-%02d-%02d %02d:%02d".format(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)
}
