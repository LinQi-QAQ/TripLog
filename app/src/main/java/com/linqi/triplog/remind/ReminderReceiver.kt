package com.linqi.triplog.remind

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.linqi.triplog.MainActivity
import com.linqi.triplog.R
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 到点发通知，并把行程标记为已提醒。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getLongExtra(ReminderScheduler.EXTRA_TRIP_ID, -1L)
        if (tripId <= 0) return
        val fallbackTitle = intent.getStringExtra(ReminderScheduler.EXTRA_TRIP_TITLE) ?: "行程"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer.of(context)
                val trip = container.tripRepository.getById(tripId)
                if (trip != null && !trip.reminded) {
                    container.tripRepository.update(trip.copy(reminded = true))
                }
                val title = trip?.title() ?: fallbackTitle
                val body = buildBody(trip, fallbackTitle)
                showNotification(context, tripId, title, body)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildBody(trip: Trip?, fallbackTitle: String): String {
        if (trip == null) return "该出发了"
        val depart = trip.departLabel()
        val num = if (trip.number.isBlank()) "" else "，车次/航班 ${trip.number}"
        val seat = if (trip.seat.isBlank()) "" else "，座位 ${trip.seat}"
        return "${trip.remindBeforeMinutes} 分钟后出发：$depart$num$seat".trim()
            .let { if (it.isBlank()) "该出发了：$fallbackTitle" else it }
    }

    private fun showNotification(context: Context, tripId: Long, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TRIP_ID, tripId)
        }
        val pi = PendingIntent.getActivity(
            context,
            tripId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (canPost) {
            manager.notify(tripId.toInt(), notification)
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "出发提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "出发前的提醒通知" }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "departure_reminder"
        const val EXTRA_OPEN_TRIP_ID = "open_trip_id"
    }
}
