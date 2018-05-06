package io.realworld.ktor

import com.soywiz.io.ktor.client.mongodb.*
import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.route.*
import kotlinx.coroutines.experimental.*
import java.security.*

fun main(args: Array<String>): Unit {

    //val user = User().apply {
    //    email = "test"
    //}

    //println(user)

    io.ktor.server.netty.DevelopmentEngine.main(args)
}

/**
 * https://www.getpostman.com/
 * https://github.com/gothinkster/realworld/blob/master/api/Conduit.postman_collection.json
 *
 * https://editor.swagger.io/
 * https://raw.githubusercontent.com/gothinkster/realworld/master/api/swagger.json
 */
@Suppress("unused")
fun Application.main() {
    runBlocking {
        val mongo = MongoDB()
        val db = mongo["realworld-sample1"]
        val users = db["users"].typed { User(it) }
            .ensureIndex(User::email to +1, unique = true)
            .ensureIndex(User::username to +1, unique = true)

        install(ContentNegotiation) {
            jackson {
            }
        }
        install(Authentication) {
            this.jwt {
                this.realm
            }
        }

        routing {
            routeAuth(users)
            get("/") {
                val user = User().apply {
                    username = "demo2"
                    email = "demo@demo.demo2"
                }
                //users.insert(user)
                //val users = users.find { User::username eq "demo2" }
                call.respondText(mapOf("user" to users).toJson())
            }
        }
    }
}

fun hashPassword(password: String): String =
    MessageDigest.getInstance("MD5").digest(password.toByteArray(Charsets.UTF_8)).hex

class PostUser(val user: PostUserUser)
class PostUserUser(val username: String, val email: String, val password: String)

class User(data: BsonDocument = mapOf()) : MongoEntity<User>(data) {
    var email: String? by Extra { null }
    //var token: String? by Extra { null }
    var username: String? by Extra { null }
    var passwordHash: String? by Extra { null }
    var bio: String? by Extra { null }
    var image: String by Extra { "dummy.png" }
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
