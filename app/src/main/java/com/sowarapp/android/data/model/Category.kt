package com.sowarapp.android.data.model

import kotlinx.serialization.Serializable

/** A group of images shown as a card on the home screen, e.g. "صباح الخير". */
@Serializable
data class Category(
    val id: String,
    val title: String,
    val coverUrl: String,
)
