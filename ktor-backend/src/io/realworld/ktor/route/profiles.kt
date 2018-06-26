package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.experimental.client.mongodb.bson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*

fun Route.routeProfiles(db: Db) {
    route("/profiles/{name}") {
        authenticate {
            route("/follow") {
                handle {
                    val userNameToFollow = call.parameters["name"]
                    val loggedUser = db.users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: notFound()
                    when (call.request.httpMethod) {
                        HttpMethod.Post -> db.users.updatePush(
                            loggedUser,
                            User::following,
                            userNameToFollow,
                            once = true
                        )
                        HttpMethod.Delete -> db.users.updatePull(loggedUser, User::following, userNameToFollow)
                        else -> notFound()
                    }
                    call.respond(db.users.findOne { User::username eq userNameToFollow }.toProfile(db, call))
                }
            }
        }
        authenticate(optional = true) {
            get {
                val name = call.parameters["name"]
                val user = db.users.findOneOrNull { User::username eq name } ?: notFound()
                call.respond(user.toProfile(db, call))
            }
        }
    }
}

suspend fun User.toProfile(db: Db, call: ApplicationCall): BsonDocument {
    return mapOf(
        "profile" to extract(
            User::username,
            User::bio,
            User::image
        ) + mapOf(
            "following" to db.userFollowing(call.getLoggedUserNameOrNull(), this.username)
        )
    )
}

suspend fun Db.userFollowing(username: String?, userToFollow: String?): Boolean {
    if (username == null || userToFollow == null) return false
    return users.find { (User::username eq username) and (User::following contains userToFollow) }.count() >= 1L
}
