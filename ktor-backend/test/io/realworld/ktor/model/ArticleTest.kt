package io.realworld.ktor.model

import org.junit.Test
import kotlin.test.*

class ArticleTest {
    @Test
    fun testSlugify() {
        assertEquals("how-to-train-your-dragon", Article.slugify("How to train your dragon"))
    }
}