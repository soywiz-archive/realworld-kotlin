package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*

fun Route.routeAuth(users: MongoDBTypedCollection<User>) {
    // Register
    post("/users") {
        val post = call.receive<PostUser>()
        try {
            val user = User().apply {
                username = post.user.username
                email = post.user.email
                passwordHash = User.hashPassword(post.user.password)
                bio = ""
            }
            users.insert(user)
            call.respond(HttpStatusCode.Created, user.userMapWithToken())
        } catch (e: Throwable) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode(422, "Unprocessable Entity"),
                mapOf("errors" to mapOf("body" to "Unexpected error"))
            )
        }
    }

    // Login
    post("/users/login") {
        val post = call.receive<UsersLoginPost>()
        val user = users.findOneOrNull {
            (User::email eq post.user.email) and
                    (User::passwordHash eq User.hashPassword(post.user.password))
        } ?: unauthorized()
        call.respond(HttpStatusCode.OK, user.userMapWithToken())
    }

    authenticate {
        // Current User
        get("/user") {
            val user = users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: unauthorized()
            call.respond(HttpStatusCode.OK, user.userMapWithToken())
        }
        // Update User
        put("/user") {
            val user = users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: unauthorized()
            val params = call.receive<Map<String, Any?>>()
            val updatableFields = listOf(User::email, User::bio, User::image)
            val updatedFields = updatableFields
                .associate { it to Dynamic { params["user"][it.name] } }
                .filterValues { it != null }
            for ((prop, value) in updatedFields) user.extra[prop.name] = value
            users.update(user, *updatedFields.keys.toTypedArray())
            call.respond(HttpStatusCode.OK, user.userMapWithToken())
        }
    }
}

private fun ApplicationCall.getLoggedUserName() = authentication.principal<UserIdPrincipal>()!!.name

private fun User.userMapWithToken() = mapOf(
    "user" to this.extract(
        User::email,
        User::username,
        User::bio,
        User::image
    ) + mapOf("token" to MyJWT.sign(username!!))
)

class PostUser(val user: PostUserUser)
class PostUserUser(val username: String, val email: String, val password: String)

class UsersLogin(val email: String, val password: String)
class UsersLoginPost(val user: UsersLogin)

