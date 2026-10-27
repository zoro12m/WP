package com.sowarapp.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One shareable image from the online catalog (catalog/catalog.json).
 *
 * [caption] is the Arabic text drawn on top of the picture, but only when [showCaption] is true.
 * Images that already have text written on them set [showCaption] to false.
 */
@Serializable
data class SowarImage(
    val id: String,
    val categoryId: String,
    /** "image" for now. Later "video"; the app skips types it doesn't know yet. */
    val type: String = TYPE_IMAGE,
    /** Full size, used on the details screen and for download/share. */
    @SerialName("url") val imageUrl: String,
    /** Small version for grids, so scrolling stays fast and cheap on data. */
    val thumbnailUrl: String = imageUrl,
    val width: Int = 0,
    val height: Int = 0,
    val caption: String = "",
    val showCaption: Boolean = true,
    /** Extra search words, e.g. "ورد" or "flowers". */
    val tags: List<String> = emptyList(),
    @SerialName("popular") val isPopular: Boolean = false,
    /** Who made the photo and its license. Some licenses require showing this. */
    val credit: Credit? = null,
) {
    /** Width / height, or 3:4 when the catalog doesn't say. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 3f / 4f

    /** What screen readers say and what search matches when there's no visible caption. */
    val description: String
        get() = caption.ifBlank { tags.firstOrNull().orEmpty() }

    companion object {
        const val TYPE_IMAGE = "image"
    }
}

@Serializable
data class Credit(
    val author: String,
    val license: String,
    val sourceUrl: String = "",
)
