package com.sowarapp.android.ui.navigation

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
