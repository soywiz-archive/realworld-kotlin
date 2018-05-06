package io.realworld

import com.soywiz.io.ktor.client.mongodb.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.DevelopmentEngine.main(args)

// https://app.swaggerhub.com/apis/soywiz3/soywiz4/
fun Application.main() {
    val mongo = MongoDB()
    val db = mongo["realworld-sample1"]
    val dummyCollection = db["dummy"]

    routing {
        get("/") {
            dummyCollection.insert(mapOf("hello" to "world"))
            call.respondText("HELLO WORLD!")
        }
    }
}

//@Serializable
data class User(
    val email: String,
    val token: String,
    val username: String,
    val bio: String,
    val image: String
)
