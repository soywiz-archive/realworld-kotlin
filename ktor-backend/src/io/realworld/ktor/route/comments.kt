package io.realworld.ktor.route

import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*
import java.util.*

fun Route.routeArticleComments(db: Db) {
    route("/articles") {
        route("/{slug}") {
            fun ApplicationCall.slug() = parameters["slug"]
            route("/comments") {
                get {
                    val comments = db.comments
                        .find { Comment::article eq call.slug() }
                        .sortedBy(Comment::updatedAt to -1)

                    call.respond(mapOf("comments" to comments.map { it.resolve(db) }.toList()))
                }
                authenticate {
                    post {
                        val post = call.receive<BsonDocument>()
                        val now = Date()
                        val comment = Comment().apply {
                            author = call.getLoggedUserName()
                            body = Dynamic { post["comment"]["body"].str }
                            createdAt = now.format(ISO8601)
                            updatedAt = now.format(ISO8601)
                            article = call.slug()
                        }

                        db.comments.insert(comment, writeConcern = mapOf("w" to 1))
                        call.respond(mapOf("comment" to comment.resolve(db)))
                    }
                    delete("/{commentId}") {
                        val commentId = call.parameters["commentId"].toString()
                        val comment = db.comments.findOne { Comment::_id eq BsonObjectId(Hex.decode(commentId)) }
                        if (comment.article != call.slug()) notFound()
                        if (comment.author != call.getLoggedUserName()) unauthorized()
                        db.comments.delete { Comment::_id eq comment._id }
                        call.respond(mapOf<String, Any?>())
                    }
                }
            }
        }
    }
}

suspend fun Comment.resolve(db: Db): BsonDocument {
    val it = this

    return extractAllBut(Comment::_id, Comment::author) + mapOf(
        "id" to it._id?.hex,
        "author" to resolveUser(this.author, db)
    )
}
