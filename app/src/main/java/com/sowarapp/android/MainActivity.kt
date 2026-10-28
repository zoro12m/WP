package com.sowarapp.android

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sowarapp.android.ui.SowarApp
import com.sowarapp.android.ui.theme.SowarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the logo splash screen until the catalog is ready (at most a moment),
        // so the user never sees an empty home screen.
        val splashScreen = installSplashScreen()
        val startTime = SystemClock.elapsedRealtime()
        val repository = (application as SowarApplication).container.imageRepository
        splashScreen.setKeepOnScreenCondition {
            repository.catalog.value == null &&
                SystemClock.elapsedRealtime() - startTime < MAX_SPLASH_MILLIS
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SowarTheme {
                // The app is Arabic-first: always lay out right-to-left,
                // even when the phone's language is English.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SowarApp()
                }
            }
        }
    }

    private companion object {
        const val MAX_SPLASH_MILLIS = 1_500L
    }
}
