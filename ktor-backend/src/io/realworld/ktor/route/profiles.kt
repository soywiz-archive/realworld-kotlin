package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*

fun Route.routeProfiles(db: Db) {
    route("/profiles/{name}") {
        route("/follow") {
            authenticate {
                post {
                    val userNameToFollow = call.parameters["name"]
                    val loggedUser = db.users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: notFound()
                    db.users.updatePush(loggedUser, User::following, userNameToFollow, once = true)
                }
                delete {
                    val userNameToFollow = call.parameters["name"]
                    val loggedUser = db.users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: notFound()
                    db.users.updatePull(loggedUser, User::following, userNameToFollow)
                }
            }
        }
        get {
            val name = call.parameters["name"]
            val user = db.users.findOneOrNull { User::username eq name } ?: notFound()
            call.respond(
                mapOf(
                    "profile" to user.extract(
                        User::username,
                        User::bio,
                        User::image
                    ) + mapOf(
                        "following" to db.userFollowing(call.getLoggedUserName(), name)
                    )
                )
            )
        }
    }
}

suspend fun Db.userFollowing(username: String?, userToFollow: String?): Boolean {
    return users.findOneOrNull { (User::username eq username) and (User::following contains userToFollow) } != null
}
