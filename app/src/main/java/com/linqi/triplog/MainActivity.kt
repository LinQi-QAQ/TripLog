package com.linqi.triplog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.linqi.triplog.di.AppContainer
import com.linqi.triplog.remind.ReminderReceiver
import com.linqi.triplog.ui.nav.AppNav
import com.linqi.triplog.ui.privacy.PrivacyDialog
import com.linqi.triplog.ui.theme.TripLogTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermissionIfNeeded()

        val openTripId = intent?.getLongExtra(ReminderReceiver.EXTRA_OPEN_TRIP_ID, -1L)
            ?.takeIf { it > 0 }

        val settings = AppContainer.of(this).settings
        // 首次启动弹出隐私政策，同意后才进主界面
        var privacyAccepted by mutableStateOf(settings.privacyAccepted)

        setContent {
            TripLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (privacyAccepted) {
                        AppNav(openTripId = openTripId)
                    } else {
                        PrivacyDialog(
                            onAccept = {
                                settings.privacyAccepted = true
                                privacyAccepted = true
                            },
                            onDecline = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
