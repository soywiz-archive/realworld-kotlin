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

    //var password: String
    //    set(value) = run { passwordHash = hashPassword(value) }
    //    get() = "****" // Can't retrieve password

    companion object {
        val HASH_ALGO = "SHA256"
        val HASH_PREFIX = "myprefix123"
        val HASH_POSTFIX = "myverylongpostfix"

        fun hashPassword(password: String): String =
            MessageDigest.getInstance(HASH_ALGO).digest("$HASH_PREFIX$password$HASH_POSTFIX".toByteArray(Charsets.UTF_8)).hex
    }
}


/*
//@Serializable
data class User(
    val email: String,
    val token: String,
    val username: String,
    val bio: String,
    val image: String
)
*/
