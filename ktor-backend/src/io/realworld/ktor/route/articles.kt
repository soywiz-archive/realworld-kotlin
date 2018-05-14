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
import java.util.*

fun Route.routeArticles(db: Db) {
    route("/articles") {
        route("/{slug}") {
            route("/comments") {
                get {
                    // @TODO: Sort by date
                    val slug = call.parameters["slug"]
                    val comments = db.comments.find { Comment::article eq slug }
                    call.respond(mapOf("comments" to comments.map { it.extractAllBut(Comment::_id) + mapOf("id" to it._id?.hex) }))
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
                        call.respond(HttpStatusCode.Conflict)
                    }
                }
            }

            get {
                val slug = call.parameters["slug"]
                val article = db.articles.findOneOrNull { Article::slug eq slug } ?: notFound()
                call.respond(mapOf("article" to article))
            }
        }

        get {
            val params = call.parameters
            val articles = when {
                params["favorited"] != null -> {
                    val user = db.users.findOneOrNull { User::username eq params["favorited"] } ?: notFound()
                    if (user.favorites != null) db.articles.find { Article::_id _in user.favorites } else listOf()

                }
                params["author"] != null -> db.articles.find { Article::author eq params["author"] }
                params["tag"] != null -> db.articles.find { Article::tagList contains params["tag"] }
                else -> db.articles.find()
            }

            call.respond(
                mapOf(
                    "articles" to articles,
                    "articlesCount" to articles.size
                )
            )
        }
    }

    authenticate {
        route("/articles/{slug}/favorite") {
            handle {
                val userName = call.getLoggedUserName()
                val slug = call.parameters["slug"]
                val user = db.users.findOneOrNull { User::username eq userName } ?: notFound("user")
                val article = db.articles.findOneOrNull { Article::slug eq slug } ?: notFound("article")
                when (call.request.httpMethod) {
                    HttpMethod.Post -> db.users.updatePush(user, User::favorites, article._id, once = true)
                    HttpMethod.Delete -> db.users.updatePull(user, User::favorites, article._id)
                    else -> notFound()
                }
                call.respond(
                    mapOf(
                        "article" to article.extractAllBut(Article::_id) + mapOf(
                            "favorited" to db.userFavorited(user.username, article.slug)
                        )
                    )
                )
            }
        }
        post("/articles") {
            val post = call.receive<BsonDocument>()
            val now = Date()
            val user = db.users.findOneOrNull { User::username eq call.getLoggedUserName() } ?: notFound("user")
            val article = Article(post["article"] as BsonDocument).apply {
                ensureNotNull(Article::title, Article::description, Article::body, Article::tagList)
                slug = Article.slugify(title ?: "")
                createdAt = ISO8601.format(now)
                updatedAt = ISO8601.format(now)
                author = user.username
                favoritesCount = 0
            }
            db.articles.insert(article, writeConcern = mapOf("w" to 1))
            call.respond(
                mapOf(
                    "article" to article.extractAllBut(Article::_id) + mapOf(
                        "favorited" to db.userFavorited(user.username, article.slug)
                    )
                )
            )
        }
    }
}

suspend fun Db.userFavorited(userName: String?, articleSlug: String?): Boolean {
    val user = users.findOneOrNull { User::username eq userName } ?: notFound("user")
    val article = articles.findOneOrNull { Article::slug eq articleSlug } ?: notFound("article")
    return user.favorites?.toList()?.contains(article._id) == true
}

/*
private suspend fun ApplicationCall.respondArticles(db: Db, filter: MongoDBTypedCollection<Article>.Expr.() -> BsonDocument) {
    val articles = db.articles.find { filter() }
    respond(
        mapOf(
            "articles" to articles,
            "articlesCount" to articles.size
        )
    )
}
*/

