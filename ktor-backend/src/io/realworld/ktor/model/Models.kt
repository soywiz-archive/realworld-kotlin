package io.realworld.ktor.model

import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import io.realworld.ktor.util.*
import java.security.*

class User(data: BsonDocument = mapOf()) : MongoEntity<User>(data) {
    var email: String? by Extra { null }
    //var token: String? by Extra { null }
    var username: String? by Extra { null }
    var passwordHash: String? by Extra { null }
    var bio: String? by Extra { null }
    var image: String by Extra { "dummy.png" }

    companion object {
        fun hashPassword(password: String): String =
            MessageDigest.getInstance("MD5").digest(password.toByteArray(Charsets.UTF_8)).hex
    }
}
