package com.sowarapp.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {

    private val catalog = CatalogParser.parse(
        """
        {
          "version": 1,
          "someFutureField": true,
          "categories": [
            {"id": "flowers", "title": "ورود", "coverUrl": "https://example.com/f.jpg"},
            {"id": "jumuah", "title": "جمعة مباركة", "coverUrl": "https://example.com/j.jpg"}
          ],
          "items": [
            {"id": "morning", "categoryId": "flowers", "url": "https://example.com/1.jpg",
             "caption": "صباح الورد", "width": 1280, "height": 960},
            {"id": "rose", "categoryId": "flowers", "url": "https://example.com/2.jpg",
             "caption": "", "showCaption": false, "tags": ["rose"], "popular": true},
            {"id": "friday", "categoryId": "jumuah", "type": "image", "url": "https://example.com/3.jpg",
             "caption": "جمعة طيبة", "credit": {"author": "Someone", "license": "CC BY 4.0"}},
            {"id": "clip", "categoryId": "jumuah", "type": "video", "url": "https://example.com/4.mp4"}
          ]
        }
        """.trimIndent()
    )

    @Test
    fun `parses categories and images and skips unknown types like video`() {
        assertEquals(2, catalog.categories.size)
        assertEquals(listOf("morning", "rose", "friday"), catalog.items.map { it.id })
        assertNull(catalog.getImage("clip"))
    }

    @Test
    fun `missing fields get sensible defaults`() {
        val morning = catalog.getImage("morning")!!
        assertTrue(morning.showCaption)
        assertEquals(morning.imageUrl, morning.thumbnailUrl)
        assertEquals(4f / 3f, morning.aspectRatio, 0.001f)
        assertEquals(3f / 4f, catalog.getImage("friday")!!.aspectRatio, 0.001f)
        assertFalse(catalog.getImage("rose")!!.showCaption)
    }

    @Test
    fun `search ignores hamza and ta marbuta differences`() {
        // "جمعه" (with ه) must still find "جمعة طيبة".
        assertEquals(listOf("friday"), catalog.search("جمعه").map { it.id })
    }

    @Test
    fun `search matches category title and tags`() {
        assertEquals(listOf("morning", "rose"), catalog.search("ورود").map { it.id })
        assertEquals(listOf("rose"), catalog.search("ROSE").map { it.id })
    }

    @Test
    fun `blank search returns nothing`() {
        assertTrue(catalog.search("   ").isEmpty())
    }

    @Test
    fun `favorites keep catalog order`() {
        val result = catalog.getImagesByIds(setOf("friday", "morning"))
        assertEquals(listOf("morning", "friday"), result.map { it.id })
    }

    @Test
    fun `popular and per-category lists`() {
        assertEquals(listOf("rose"), catalog.getPopularImages().map { it.id })
        assertEquals(listOf("friday"), catalog.getImagesInCategory("jumuah").map { it.id })
    }

    @Test
    fun `normalize removes diacritics`() {
        assertEquals("من جد وجد", ArabicText.normalize("مَنْ جدّ وجد"))
    }

    /** Checks the real catalog/catalog.json that ships with the app and is served online. */
    @Test
    fun `bundled catalog is valid`() {
        val file = File("../catalog/catalog.json")
        if (!file.exists()) return
        val real = CatalogParser.parse(file.readText())
        assertTrue(real.categories.isNotEmpty())
        val ids = real.items.map { it.id }
        assertEquals("image ids must be unique", ids.size, ids.toSet().size)
        real.categories.forEach { category ->
            assertTrue(category.title, real.getImagesInCategory(category.id).isNotEmpty())
        }
        real.items.forEach { image ->
            assertTrue(image.id, image.imageUrl.startsWith("https://"))
            assertTrue(image.id, real.getCategory(image.categoryId) != null)
        }
    }
}
