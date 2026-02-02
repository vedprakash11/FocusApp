package com.ved.focusapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ved.focusapp.data.PreferencesStorage
import com.ved.focusapp.dnd.DndHelper
import com.ved.focusapp.notification.NotificationHelper
import com.ved.focusapp.timer.AlarmScheduler
import com.ved.focusapp.timer.TimerEngine
import com.ved.focusapp.timer.TimerStateHolder
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ved.focusapp.ui.FocusAppNav
import com.ved.focusapp.ui.splash.SplashScreen
import com.google.android.gms.ads.MobileAds
import com.ved.focusapp.ui.theme.FocusAppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var storage: PreferencesStorage
    private lateinit var engine: TimerEngine
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var dndHelper: DndHelper
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var timerViewModel: TimerStateHolder

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* result not required for basic flow */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) { }

        val app = applicationContext
        storage = PreferencesStorage(app)
        engine = TimerEngine()
        notificationHelper = NotificationHelper(app)
        dndHelper = DndHelper(getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
        alarmScheduler = AlarmScheduler(app)

        val factory = TimerStateHolder.Factory(app, storage, engine, notificationHelper, dndHelper, alarmScheduler)
        timerViewModel = ViewModelProvider(this, factory)[TimerStateHolder::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            FocusAppTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2200)
                    showSplash = false
                }
                Crossfade(
                    targetState = showSplash,
                    label = "splash_to_main"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else {
                        FocusAppNav(
                            viewModel = timerViewModel,
                            storage = storage
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timerViewModel.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        timerViewModel.onAppResume()
    }
}
