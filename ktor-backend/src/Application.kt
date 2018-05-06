package io.realworld

import com.soywiz.io.ktor.client.mongodb.*
import com.soywiz.io.ktor.client.mongodb.bson.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.experimental.*

fun main(args: Array<String>): Unit {

    //val user = User().apply {
    //    email = "test"
    //}

    //println(user)

    io.ktor.server.netty.DevelopmentEngine.main(args)
}

// https://app.swaggerhub.com/apis/soywiz3/soywiz4/
fun Application.main() {
    runBlocking {
        val mongo = MongoDB()
        val db = mongo["realworld-sample1"]
        val users = db["users"].typed { User(it) }
            .ensureIndex(User::email to +1, unique = true)
            .ensureIndex(User::username to +1, unique = true)

        routing {
            get("/") {
                val user = User().apply {
                    username = "demo2"
                    email = "demo@demo.demo2"
                }
                users.insert(user)
                val users = users.find { User::username eq "demo2" }
                call.respondText(mapOf("user" to users).toJson())
            }
        }
    }
}

class User(data: BsonDocument = mapOf()) : MongoEntity<User>(data) {
    var email: String? by Extra { null }
    var token: String? by Extra { null }
    var username: String? by Extra { null }
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
