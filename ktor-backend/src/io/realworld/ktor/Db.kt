package io.realworld.ktor

import io.ktor.experimental.client.mongodb.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*

class Db private constructor(mongo: MongoDB) {
    val db = mongo["realworld-sample1"]
    lateinit var users: MongoDBTypedCollection<User>; private set
    lateinit var articles: MongoDBTypedCollection<Article>; private set
    lateinit var comments: MongoDBTypedCollection<Comment>; private set

    companion object {
        suspend operator fun invoke(host: String = "127.0.0.1", port: Int = 27017): Db = Db(MongoDB(host, port)).apply {
            users = db["users"].typed { User(it) }
                .ensureIndex(User::email to +1, unique = true)
                .ensureIndex(User::username to +1, unique = true)

            articles = db["articles"].typed { Article(it) }
                .ensureIndex(Article::slug to +1, unique = true)
                .ensureIndex(Article::updatedAt to -1)
                .ensureIndex(Article::tagList to +1, Article::updatedAt to -1, name = "tag_updated")
                .ensureIndex(Article::author to +1, Article::updatedAt to -1, name = "author_updated")

            comments = db["comments"].typed { Comment(it) }
        }
    }
}