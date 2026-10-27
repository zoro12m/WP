package com.sowarapp.android.data

import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage

/** The single place screens ask for categories and images. */
class ImageRepository(private val dataSource: LocalImageDataSource) {

    fun getCategories(): List<Category> = dataSource.categories

    fun getCategory(id: String): Category? = dataSource.categories.find { it.id == id }

    fun getImagesInCategory(categoryId: String): List<SowarImage> =
        dataSource.images.filter { it.categoryId == categoryId }

    fun getPopularImages(): List<SowarImage> = dataSource.images.filter { it.isPopular }

    fun getImage(id: String): SowarImage? = dataSource.images.find { it.id == id }

    /** Keeps the catalog order so favorites don't jump around. */
    fun getImagesByIds(ids: Set<String>): List<SowarImage> =
        dataSource.images.filter { it.id in ids }

    /** Matches the query against the image caption and its category title. */
    fun search(query: String): List<SowarImage> {
        val normalizedQuery = ArabicText.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val categoryTitles = dataSource.categories.associate { it.id to it.title }
        return dataSource.images.filter { image ->
            val caption = ArabicText.normalize(image.caption)
            val categoryTitle = ArabicText.normalize(categoryTitles[image.categoryId].orEmpty())
            normalizedQuery in caption || normalizedQuery in categoryTitle
        }
    }
}
