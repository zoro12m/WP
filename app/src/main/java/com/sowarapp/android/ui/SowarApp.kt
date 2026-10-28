package com.sowarapp.android.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sowarapp.android.R
import com.sowarapp.android.ui.navigation.Routes
import com.sowarapp.android.ui.navigation.SowarNavHost

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Routes.FAVORITES, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Filled.FavoriteBorder),
)

/** The root of the UI: a floating bottom navigation bar + the screen currently shown. */
@Composable
fun SowarApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The bottom bar only appears on the two main tabs, not on Category or Details.
    val showBottomBar = bottomTabs.any { it.route == currentDestination?.route }

    // Status bar icons: white over the rose header and over photos, dark only on the light Favorites page.
    val darkTheme = isSystemInDarkTheme()
    val view = LocalView.current
    val lightStatusBar = !darkTheme && currentDestination?.route == Routes.FAVORITES
    LaunchedEffect(lightStatusBar) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusBar
    }

    Scaffold(
        // Each screen handles the status bar itself, so it can draw its header behind it.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                FloatingBottomBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Standard tab behaviour: don't stack copies of the same tab,
                                    // and remember each tab's scroll position.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        SowarNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

/** A rounded "pill" bar that floats above the bottom edge of the screen. */
@Composable
private fun FloatingBottomBar(content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 40.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            content = content,
        )
    }
}
