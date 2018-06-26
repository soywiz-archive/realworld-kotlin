package io.realworld.ktor.model

import org.junit.*
import org.junit.Assert.*

class ArticleTest {
    @Test
    fun testSlugify() {
        assertEquals("how-to-train-your-dragon", Article.slugify("How to train your dragon"))
    }
}