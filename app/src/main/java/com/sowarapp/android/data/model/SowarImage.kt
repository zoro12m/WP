package com.sowarapp.android.data.model

import androidx.annotation.DrawableRes

/**
 * One shareable image.
 *
 * The background is a local drawable and [caption] is the Arabic text written on top of it.
 * Later, when images come from the internet, [imageRes] will be replaced by a URL.
 */
data class SowarImage(
    val id: String,
    val categoryId: String,
    @DrawableRes val imageRes: Int,
    val caption: String,
    val isPopular: Boolean = false,
)
