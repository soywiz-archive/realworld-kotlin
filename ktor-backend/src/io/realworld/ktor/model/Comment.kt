package io.realworld.ktor.model

import io.ktor.experimental.client.mongodb.bson.*
import io.realworld.ktor.util.*

class Comment(data: BsonDocument = mapOf()) : MongoEntity<Comment>(data) {
    var body: String? by Extra { null }
    var createdAt: String? by Extra { null }
    var updatedAt: String? by Extra { null }
    var author: String? by Extra { null }
    var article: String? by Extra { null }
}
