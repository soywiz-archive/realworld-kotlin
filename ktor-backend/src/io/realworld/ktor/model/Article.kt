package io.realworld.ktor.model

import com.soywiz.io.ktor.client.mongodb.bson.*
import io.realworld.ktor.util.*
import java.text.*

class Article(data: BsonDocument = mapOf()) : MongoEntity<Article>(data) {
    var title: String? by Extra { null }
    var author: String? by Extra { null }
    var slug: String? by Extra { null }
    var createdAt: String? by Extra { null }
    var updatedAt: String? by Extra { null }
    var description: String? by Extra { null }
    var body: String? by Extra { null }
    var tagList: List<String?>? by Extra { null }
    var favoritesCount: Long? by Extra { null }

    companion object {
        fun slugify(title: String): String = title.replace(Regex("\\W+"), "-").toLowerCase()
    }
}
