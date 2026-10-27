package com.sowarapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.sowarapp.android.ui.SowarApp
import com.sowarapp.android.ui.theme.SowarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
}
