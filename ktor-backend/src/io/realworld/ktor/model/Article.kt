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
    var favorited: Boolean? by Extra { null }
    var favoritesCount: Long? by Extra { null }

    companion object {
        // "2018-05-07T06:36:02.416Z"
        val ISO8601 = SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'")

        fun slugify(title: String): String = title.replace(Regex("\\W+"), "-").toLowerCase()
    }
}
