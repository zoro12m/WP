package com.sowarapp.android.ui.navigation

/** Every screen has a route, like a web address inside the app. */
object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"

    const val ARG_CATEGORY_ID = "categoryId"
    const val CATEGORY = "category/{$ARG_CATEGORY_ID}"
    fun category(categoryId: String) = "category/$categoryId"

    const val ARG_IMAGE_ID = "imageId"
    const val IMAGE_DETAILS = "image/{$ARG_IMAGE_ID}"
    fun imageDetails(imageId: String) = "image/$imageId"
}
