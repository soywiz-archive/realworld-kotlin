package io.realworld.ktor

import com.soywiz.io.ktor.client.mongodb.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*

class Db private constructor(dummy: Boolean) {
    val mongo = MongoDB()
    val db = mongo["realworld-sample1"]
    lateinit var users: MongoDBTypedCollection<User>; private set
    lateinit var articles: MongoDBTypedCollection<Article>; private set

    companion object {
        suspend operator fun invoke(): Db = Db(true).apply {
            users = db["users"].typed { User(it) }
                .ensureIndex(User::email to +1, unique = true)
                .ensureIndex(User::username to +1, unique = true)
            articles = db["articles"].typed { Article(it) }
        }
    }
}