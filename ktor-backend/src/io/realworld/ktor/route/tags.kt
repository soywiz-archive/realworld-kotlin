package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*

fun Route.routeTags(db: Db) {
    get("/tags") {
        call.respond(mapOf(
            "tags" to (db.articles.distinct(Article::tagList) as List<Any?>).filterIsInstance<String>()
        ))
    }
}