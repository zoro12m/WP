package com.sowarapp.android.data.model

import com.sowarapp.android.data.ArabicText
import kotlinx.serialization.Serializable

/** The shape of catalog/catalog.json, exactly as it is stored online. */
@Serializable
data class CatalogFile(
    val version: Int = 1,
    val categories: List<Category> = emptyList(),
    @Serializable(with = ImageListSerializer::class)
    val items: List<SowarImage> = emptyList(),
)

/**
 * Everything the app can show, with lookup tables built once
 * so screens stay fast even with thousands of images.
 */
class Catalog(
    val categories: List<Category>,
    val items: List<SowarImage>,
) {
    constructor(file: CatalogFile) : this(file.categories, file.items)

    private val imagesById: Map<String, SowarImage> = items.associateBy { it.id }
    private val imagesByCategory: Map<String, List<SowarImage>> = items.groupBy { it.categoryId }
    private val categoriesById: Map<String, Category> = categories.associateBy { it.id }

    /** Normalized caption + tags + category title of each image, for search. */
    private val searchText: List<Pair<SowarImage, String>> = items.map { image ->
        val words = listOf(image.caption, categoriesById[image.categoryId]?.title.orEmpty()) + image.tags
        image to ArabicText.normalize(words.joinToString(" "))
    }

    fun getCategory(id: String): Category? = categoriesById[id]

    fun getImagesInCategory(categoryId: String): List<SowarImage> = imagesByCategory[categoryId].orEmpty()

    fun getPopularImages(): List<SowarImage> = items.filter { it.isPopular }

    fun getImage(id: String): SowarImage? = imagesById[id]

    /** Keeps the catalog order so favorites don't jump around. */
    fun getImagesByIds(ids: Set<String>): List<SowarImage> = items.filter { it.id in ids }

    /** Matches the query against the caption, tags and category title. */
    fun search(query: String): List<SowarImage> {
        val normalizedQuery = ArabicText.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        return searchText.filter { (_, text) -> normalizedQuery in text }.map { it.first }
    }

    companion object {
        val EMPTY = Catalog(emptyList(), emptyList())
    }
}
