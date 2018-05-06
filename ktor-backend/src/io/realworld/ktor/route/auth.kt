package io.realworld.route

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*


fun Route.routeAuth(users: MongoDBTypedCollection<User>) {
    post("/users") {
        val post = call.receive<PostUser>()
        try {
            val user = User().apply {
                username = post.user.username
                email = post.user.email
                passwordHash = hashPassword(post.user.password)
                bio = ""
            }
            users.insert(user)
            call.respond(
                HttpStatusCode.Created, mapOf("user" to mapOf(
                    User::email.name to user.email,
                    User::username.name to user.username,
                    User::bio.name to user.bio,
                    User::image.name to user.image,
                    "token" to "token"
                )
                )
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode(422, "Unprocessable Entity"),
                mapOf("errors" to mapOf("body" to "Unexpected error"))
            )
        }
    }

    authenticate {
        get("/user") {
            call.respond(
                HttpStatusCode.OK, mapOf("user" to mapOf(
                    User::email.name to "email",
                    User::username.name to "username",
                    User::bio.name to "bio",
                    User::image.name to "image",
                    "token" to "token"
                )
                )
            )
        }
    }
}
