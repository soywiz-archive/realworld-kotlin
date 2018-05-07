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
    get("/articles/{slug}") {
        val slug = call.parameters["slug"]
        val article = db.articles.findOneOrNull { Article::slug eq slug } ?: notFound()
        call.respond(mapOf("article" to article))
    }
    route("/articles") {
        param("tag") {
            get {
                val tag = call.parameters["tag"]
                val articles = db.articles.find { Article::tagList contains tag }
                call.respond(
                    mapOf(
                        "articles" to articles,
                        "articlesCount" to articles.size
                    )
                )
            }
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
