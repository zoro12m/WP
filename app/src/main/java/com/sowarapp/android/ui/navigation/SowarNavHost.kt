package com.sowarapp.android.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sowarapp.android.ui.category.CategoryScreen
import com.sowarapp.android.ui.details.ImageDetailsScreen
import com.sowarapp.android.ui.favorites.FavoritesScreen
import com.sowarapp.android.ui.home.HomeScreen

@Composable
fun SowarNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openImage: (String) -> Unit = { imageId -> navController.navigate(Routes.imageDetails(imageId)) }
    val goBack: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        // Screens gently fade and grow in, and fade and shrink away when going back.
        enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) + scaleIn(tween(TRANSITION_MILLIS), initialScale = 0.96f) },
        exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MILLIS)) + scaleOut(tween(TRANSITION_MILLIS), targetScale = 0.96f) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onCategoryClick = { categoryId -> navController.navigate(Routes.category(categoryId)) },
                onImageClick = openImage,
            )
        }
        composable(
            route = Routes.CATEGORY,
            arguments = listOf(navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }),
        ) {
            CategoryScreen(onBack = goBack, onImageClick = openImage)
        }
        composable(
            route = Routes.IMAGE_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_IMAGE_ID) { type = NavType.StringType }),
        ) {
            ImageDetailsScreen(onBack = goBack)
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(onImageClick = openImage)
        }
    }
}

private const val TRANSITION_MILLIS = 250
