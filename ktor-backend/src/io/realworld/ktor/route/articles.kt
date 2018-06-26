package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.experimental.client.mongodb.bson.*
import io.ktor.experimental.client.util.*
import io.ktor.http.*
import io.ktor.pipeline.*
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
            fun ApplicationCall.slug() = parameters["slug"]
            get {
                val slug = call.slug()
                val article = db.articles.findOne { Article::slug eq slug }
                call.respond(mapOf("article" to article.resolve(call, db)))
            }
            authenticate {
                put {
                    val slug = call.slug()
                    val params = call.receive<BsonDocument>()
                    val paramsArticle = params["article"]
                    val article = db.articles.findOne { Article::slug eq slug }
                    db.articles.update(
                        article.apply {
                            body = Dynamic { paramsArticle["body"].str }
                            description = Dynamic { paramsArticle["description"].str }
                            tagList = Dynamic { paramsArticle["tagList"].list.map { it.str } }
                            title = Dynamic { paramsArticle["title"].str }
                            updatedAt = ISO8601.format(Date())
                        },
                        Article::body, Article::description, Article::tagList, Article::title, Article::updatedAt
                    )
                    call.respond(mapOf("article" to article.resolve(call, db)))
                }
            }
        }

        authenticate(optional = true) {
            get {
                handleFeed(db, feed = false)
            }

            // People following
            get("/feed") {
                handleFeed(db, feed = true)
            }
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
                    else -> methodNotAllowed()
                }

                article.favoritesCount = db.users.query().filter { User::favorites contains article._id }.count()
                db.articles.update(article, Article::favoritesCount)

                call.respond(mapOf("article" to article.resolve(call, db)))
            }
        }
        post("/articles") {
            val post = call.receive<BsonDocument>()
            val now = Date()
            val user = db.users.findOne { User::username eq call.getLoggedUserName() }
            val article = Article(post["article"] as BsonDocument).apply {
                ensureNotNull(Article::title, Article::description, Article::body, Article::tagList)
                slug = Article.slugify(title ?: "")
                createdAt = ISO8601.format(now)
                updatedAt = ISO8601.format(now)
                author = user.username
                favoritesCount = 0
            }
            db.articles.insert(article, writeConcern = mapOf("w" to 1))
            call.respond(mapOf("article" to article.resolve(call, db)))
        }
    }
}

suspend fun Db.userFavorited(userName: String?, articleSlug: String?): Boolean {
    val user = users.findOneOrNull { User::username eq userName } ?: notFound("user")
    val article = articles.findOneOrNull { Article::slug eq articleSlug } ?: notFound("article")
    return user.favorites?.toList()?.contains(article._id) == true
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleFeed(db: Db, feed: Boolean) {
    val params = call.parameters

    val articles = when {
        feed -> {
            val loggedUser = db.users.findOne { User::username eq call.getLoggedUserName() }
            val following = loggedUser.following ?: listOf()
            db.articles.find { Article::author _in following }
        }
        params["favorited"] != null -> {
            val user = db.users.findOneOrNull { User::username eq params["favorited"] } ?: notFound()
            if (user.favorites != null) db.articles.find { Article::_id _in user.favorites } else db.articles.find { Article::_id eq "invalid" }

        }
        params["author"] != null -> db.articles.find { Article::author eq params["author"] }
        params["tag"] != null -> db.articles.find { Article::tagList contains params["tag"] }
        else -> db.articles.query()
    }

    val limit = (params["limit"]?.toIntOrNull() ?: 10).clamp(1, 20)
    val offset = (params["offset"]?.toIntOrNull() ?: 0).clamp(0, 5000)

    call.respond(
        mapOf(
            "articles" to articles
                .skip(offset).limit(limit)
                .sortedBy(Article::updatedAt to -1)
                .map { it.resolve(call, db) }
                .toList()
            ,
            "articlesCount" to articles.count()
        )
    )
}

suspend fun Article.resolve(call: ApplicationCall, db: Db): BsonDocument {
    // @TODO: Cache or resolve all users at once
    val article = this

    //println("------------------------------------")
    //println(author)
    //println(article._id)
    //println(call.getLoggedUserNameOrNull())
    return this.extractAllBut(Article::_id) + mapOf(
        "author" to resolveUser(this.author, db),
        "favorited" to (db.users.query().filter {
            (User::username eq call.getLoggedUserNameOrNull()) and (User::favorites contains article._id)
        }.count() != 0L)
    )
}
