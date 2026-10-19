package com.linqi.triplog.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.linqi.triplog.MainActivity
import com.linqi.triplog.R
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小组件：显示最近一趟即将出发的行程。
 *
 * 数据直接从 AppContainer 里的仓库取，和 app 内是同一份数据库，
 * 所以在 app 里改了什么，小组件刷新后立刻同步。
 *
 * 刷新时机有三种：
 * 1. 系统按 updatePeriodMillis 定期触发（最短 30 分钟，这是 Android 的硬下限）
 * 2. 用户添加或调整小组件尺寸时触发 onAppWidgetOptionsChanged
 * 3. app 内部数据变化时主动调用 [TripWidget.refresh]
 *
 * 倒计时做不到秒级实时，因为 RemoteViews 不能跑定时器。
 * 显示到「几分钟后」「几小时后」这个粒度，配合 30 分钟一次的刷新，
 * 用户感知上够用，也不会耗电。
 */
class TripWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            render(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        render(context, appWidgetManager, appWidgetId)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        // 数据库查询是 IO，放到后台线程。RemoteViews 的更新要回主线程。
        // goAsync 在这里没必要，直接用一个短生命周期的协程即可，
        // 因为查询很快，且 AppWidgetProvider 的进程不会立刻被回收。
        CoroutineScope(Dispatchers.IO).launch {
            val trip = runCatching {
                val repo = AppContainer.of(context).tripRepository
                pickNextTrip(repo.getAllSnapshot(), System.currentTimeMillis())
            }.getOrNull()

            val views = buildViews(context, trip)
            CoroutineScope(Dispatchers.Main).launch {
                manager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {

        /**
         * 从全部行程里挑出「下一趟」：出发时间在未来的最近一条。
         * 出发时间为空的记录（只录了站名没填时间的）不参与，
         * 因为没法判断早晚，拿来显示倒计时也没有意义。
         */
        fun pickNextTrip(trips: List<Trip>, now: Long): Trip? =
            trips.asSequence()
                .filter { it.departTime != null && it.departTime > now }
                .minByOrNull { it.departTime!! }

        /** 手动刷新所有正在桌面上的小组件。app 内数据变动后调用。 */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TripWidget::class.java)
            )
            if (ids.isEmpty()) return

            val intent = Intent(context, TripWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun buildViews(context: Context, trip: Trip?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_trip)

            // 整块可点击，点了打开 app
            views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))

            if (trip == null) {
                views.setViewVisibility(R.id.widget_number, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_route, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_time, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_countdown, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_icon, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                return views
            }

            views.setViewVisibility(R.id.widget_number, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_route, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_time, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_countdown, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_icon, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)

            // 第一行：车次或航班号。没填就用交通工具类别兜底
            val number = trip.number.ifBlank { vehicleLabel(trip.vehicleType) }
            views.setTextViewText(R.id.widget_number, number)

            // 第二行：出发地 → 目的地
            val route = "${trip.fromStation.ifBlank { "—" }} → ${trip.toStation.ifBlank { "—" }}"
            views.setTextViewText(R.id.widget_route, route)

            // 第三行：出发时间
            val depart = trip.departTime
            val timeText = if (depart != null) {
                SimpleDateFormat("M月d日 EEE HH:mm", Locale.CHINA).format(Date(depart))
            } else {
                ""
            }
            views.setTextViewText(R.id.widget_time, timeText)

            // 第四行：倒计时
            val countdown = if (depart != null) {
                formatCountdown(depart - System.currentTimeMillis())
            } else {
                ""
            }
            views.setTextViewText(R.id.widget_countdown, countdown)

            views.setImageViewResource(R.id.widget_icon, iconFor(trip.vehicleType))

            return views
        }

        /**
         * 倒计时文案。
         * 不足一小时精确到分钟，一天以内精确到小时，再远就给天数。
         * 这样越临近越精确，用户看得更有感觉。
         */
        fun formatCountdown(deltaMillis: Long): String {
            if (deltaMillis <= 0) return "已出发"

            val minutes = deltaMillis / 60_000
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> "不到 1 分钟"
                minutes < 60 -> "$minutes 分钟后"
                hours < 24 -> "$hours 小时后"
                else -> {
                    // 超过一天时，余下的小时数也带上，信息更完整
                    val restHours = hours % 24
                    if (restHours > 0) "$days 天 $restHours 小时后" else "$days 天后"
                }
            }
        }

        private fun iconFor(vehicleType: String): Int = when (vehicleType) {
            VehicleType.TRAIN -> R.drawable.ic_widget_train
            VehicleType.FLIGHT -> R.drawable.ic_widget_flight
            VehicleType.BUS -> R.drawable.ic_widget_bus
            else -> R.drawable.ic_widget_other
        }

        private fun vehicleLabel(vehicleType: String): String = when (vehicleType) {
            VehicleType.TRAIN -> "火车"
            VehicleType.FLIGHT -> "航班"
            VehicleType.BUS -> "汽车"
            else -> "行程"
        }

        private fun launchIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            // FLAG_IMMUTABLE 是 Android 12 起的要求，低版本忽略
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
