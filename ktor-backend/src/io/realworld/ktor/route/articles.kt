package io.realworld.ktor.route

import com.soywiz.io.ktor.client.mongodb.bson.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*
import java.util.*

fun Route.routeArticles(db: Db) {

    route("/articles") {
        get("/{slug}") {
            val slug = call.parameters["slug"]
            val article = db.articles.findOneOrNull { Article::slug eq slug } ?: notFound()
            call.respond(mapOf("article" to article))
        }
        get {
            val articles = db.articles.find {
                when {
                    call.parameters["favorited"] != null -> Article::author eq call.parameters["favorited"]
                    call.parameters["author"] != null -> Article::author eq call.parameters["author"]
                    call.parameters["tag"] != null -> Article::tagList contains call.parameters["tag"]
                    else -> all()
                }
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
        post("/articles") {
            val receive = call.receive<BsonDocument>()
            val now = Date()
            val article = Article(receive["article"] as BsonDocument).apply {
                ensureNotNull(Article::title, Article::description, Article::body, Article::tagList)
                slug = Article.slugify(title ?: "")
                createdAt = Article.ISO8601.format(now)
                updatedAt = Article.ISO8601.format(now)
                author = call.getLoggedUserName()
                favorited = false
                favoritesCount = 0
            }
            db.articles.insert(article)
            call.respond(mapOf("article" to article))
        }
    }
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
