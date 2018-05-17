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
            route("/comments") {
                get {
                    // @TODO: Sort by date
                    val slug = call.parameters["slug"]
                    val comments = db.comments.find { Comment::article eq slug }
                    call.respond(mapOf("comments" to comments.map { it.extractAllBut(Comment::_id) + mapOf("id" to it._id?.hex) }.toList()))
                }
                authenticate {
                    post {
                        val slug = call.parameters["slug"]
                        val post = call.receive<BsonDocument>()
                        val now = Date()
                        val comment = Comment().apply {
                            author = call.getLoggedUserName()
                            body = Dynamic { post["comment"]["body"].str }
                            createdAt = ISO8601.format(now)
                            updatedAt = ISO8601.format(now)
                            article = slug
                        }

                        db.comments.insert(comment, writeConcern = mapOf("w" to 1))
                        call.respond(mapOf("comment" to comment.extractAllBut(Comment::_id) + mapOf("id" to comment._id?.hex)))
                    }
                    delete("/{commentId}") {
                        val slug = call.parameters["slug"]
                        val commentId = call.parameters["commentId"].toString()
                        val comment = db.comments.findOneOrNull { Comment::_id eq BsonObjectId(Hex.decode(commentId)) }
                                ?: notFound()
                        if (comment.article != slug) notFound()
                        if (comment.author != call.getLoggedUserName()) unauthorized()
                        db.comments.delete { Comment::_id eq comment._id }
                        call.respond(mapOf("ok" to "ok"))
                    }
                }
            }
        }
    }
}
