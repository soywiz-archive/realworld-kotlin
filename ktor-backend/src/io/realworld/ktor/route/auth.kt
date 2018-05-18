package io.realworld.ktor.route

import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*
import java.security.*

fun Route.routeAuth(db: Db, myjwt: MyJWT) {
    val users = db.users

    // Register
    post("/users") {
        val post = call.receive<PostUser>()
        try {
            val user = User().apply {
                username = post.user.username
                email = post.user.email
                passwordHash = User.hashPassword(post.user.password)
                bio = ""
                //image = "https://static.productionready.io/images/smiley-cyrus.jpg"
                val emailMd5 =
                    Hex.encodeLower(MessageDigest.getInstance("MD5").digest((email ?: "").toByteArray(Charsets.UTF_8)))
                image = "https://s.gravatar.com/avatar/$emailMd5?s=80"
            }
            users.insert(user)
            call.respond(HttpStatusCode.Created, user.userMapWithToken(myjwt))
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
        call.respond(HttpStatusCode.OK, user.userMapWithToken(myjwt))
    }

    authenticate {
        route("/user") {
            // Current User
            get {
                val user = users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: unauthorized()
                call.respond(HttpStatusCode.OK, user.userMapWithToken(myjwt))
            }
            // Update User
            put {
                val user = users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: unauthorized()
                val params = call.receive<BsonDocument>()
                val updatableFields = listOf(User::email, User::bio, User::image)
                val updatedFields = updatableFields
                    .associate { it to Dynamic { params["user"][it.name] } }
                    .filterValues { it != null }
                for ((prop, value) in updatedFields) user.extra[prop.name] = value
                users.update(user, *updatedFields.keys.toTypedArray())
                call.respond(HttpStatusCode.OK, user.userMapWithToken(myjwt))
            }
        }
    }
}

private fun User.userMapWithToken(myjwt: MyJWT) = mapOf(
    "user" to this.extract(User::email, User::username, User::bio, User::image) + mapOf(
        "token" to myjwt.sign(username!!)
    )
)

class PostUser(val user: PostUserUser)
class PostUserUser(val username: String, val email: String, val password: String)

class UsersLogin(val email: String, val password: String)
class UsersLoginPost(val user: UsersLogin)

