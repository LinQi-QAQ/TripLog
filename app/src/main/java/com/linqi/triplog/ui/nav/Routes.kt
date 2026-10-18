package com.linqi.triplog.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航目标。 */
sealed class Dest(val route: String) {
    data object List : Dest("list")
    data object Me : Dest("me")
}

object Routes {
    const val LIST = "list"
    const val ME = "me"
    const val AI_SETTINGS = "ai_settings"
    const val BACKUP = "backup"
    const val STATS = "stats"
    const val DETAIL = "detail/{tripId}"
    const val ADD = "add?tripId={tripId}"

    fun detail(tripId: Long): String = "detail/$tripId"

    fun add(tripId: Long? = null): String =
        if (tripId != null && tripId > 0) "add?tripId=$tripId" else "add"
}

/** 集中放图标，避免业务代码里散落具体引用。 */
object Icons {
    val add: ImageVector = Icons.Filled.Add
    val calendar: ImageVector = Icons.Outlined.DateRange
    val calendarFilled: ImageVector = Icons.Filled.DateRange
    val person: ImageVector = Icons.Outlined.Person
    val personFilled: ImageVector = Icons.Filled.Person
    val sparkle: ImageVector = Icons.Outlined.AutoAwesome
    val edit: ImageVector = Icons.Filled.Edit
    val delete: ImageVector = Icons.Filled.Delete
    val search: ImageVector = Icons.Filled.Search
    val settings: ImageVector = Icons.Filled.Settings
    val share: ImageVector = Icons.Filled.Share
}
