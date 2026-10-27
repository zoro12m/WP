package com.sowarapp.android.data.model

import androidx.annotation.DrawableRes

/** A group of images shown as a card on the home screen, e.g. "صباح الخير". */
data class Category(
    val id: String,
    val title: String,
    @DrawableRes val coverRes: Int,
)
