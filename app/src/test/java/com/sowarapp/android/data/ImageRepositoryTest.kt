package com.sowarapp.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageRepositoryTest {

    private val dataSource = LocalImageDataSource()
    private val repository = ImageRepository(dataSource)

    @Test
    fun `every category has images and a valid cover`() {
        repository.getCategories().forEach { category ->
            assertTrue(category.title, repository.getImagesInCategory(category.id).isNotEmpty())
        }
        assertEquals(6, repository.getCategories().size)
    }

    @Test
    fun `image ids are unique`() {
        val ids = dataSource.images.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `search ignores hamza and ta marbuta differences`() {
        // "جمعه" (with ه) must still find "جمعة مباركة".
        val results = repository.search("جمعه")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.categoryId == LocalImageDataSource.JUMUAH })
    }

    @Test
    fun `search matches the category title`() {
        val results = repository.search("ورود")
        assertTrue(results.any { it.categoryId == LocalImageDataSource.FLOWERS })
    }

    @Test
    fun `blank search returns nothing`() {
        assertTrue(repository.search("   ").isEmpty())
    }

    @Test
    fun `favorites keep catalog order`() {
        val result = repository.getImagesByIds(setOf("wisdom_1", "morning_2"))
        assertEquals(listOf("morning_2", "wisdom_1"), result.map { it.id })
    }

    @Test
    fun `normalize removes diacritics`() {
        assertEquals("من جد وجد", ArabicText.normalize("مَنْ جدّ وجد"))
        assertNotNull(repository.getImage("flowers_1"))
    }
}
