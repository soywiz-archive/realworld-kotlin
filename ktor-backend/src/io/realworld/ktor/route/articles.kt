package io.realworld.ktor.route

import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.pipeline.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.*
import io.realworld.ktor.model.*
import io.realworld.ktor.util.*
import kotlinx.coroutines.experimental.*
import java.net.*
import java.util.*

fun Route.routeArticles(db: Db) {
    route("/articles") {
        route("/{slug}") {
            get {
                val slug = call.parameters["slug"]
                val article = db.articles.findOneOrNull { Article::slug eq slug } ?: notFound()
                call.respond(mapOf("article" to article.resolve(call, db)))
            }
        }

        get {
            handleFeed(db)
        }

        // People following
        get("/feed") {
            handleFeed(db)
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

                article.favoritesCount = db.users.query().filter { User::favorites contains article._id }.count()
                db.articles.update(article, Article::favoritesCount)

                call.respond(mapOf("article" to article.resolve(call, db)))
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
            call.respond(mapOf("article" to article.resolve(call, db)))
        }
    }
}

suspend fun Db.userFavorited(userName: String?, articleSlug: String?): Boolean {
    val user = users.findOneOrNull { User::username eq userName } ?: notFound("user")
    val article = articles.findOneOrNull { Article::slug eq articleSlug } ?: notFound("article")
    return user.favorites?.toList()?.contains(article._id) == true
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleFeed(db: Db) {
    val params = call.parameters
    val articleQuery = db.articles.query()

    val articles = when {
        params["favorited"] != null -> {
            val user = db.users.findOneOrNull { User::username eq params["favorited"] } ?: notFound()
            if (user.favorites != null) articleQuery.filter { Article::_id _in user.favorites } else articleQuery.filter { Article::_id eq "invalid" }

        }
        params["author"] != null -> articleQuery.filter { Article::author eq params["author"] }
        params["tag"] != null -> articleQuery.filter { Article::tagList contains params["tag"] }
        else -> articleQuery
    }

    val limit = (params["limit"]?.toIntOrNull() ?: 10).clamp(1, 20)
    val offset = params["offset"]?.toIntOrNull() ?: 0

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
    val authorName = this.author
    val author = db.users.query()
        .include(User::username, User::bio, User::image, User::favorites)
        .filter { User::username eq authorName }
        .firstOrNull() ?: User(mapOf())

    //println("------------------------------------")
    //println(author)
    //println(article._id)
    //println(call.getLoggedUserNameOrNull())
    return this.extractAllBut(Article::_id) + mapOf(
        "author" to mapOf(
            "username" to author.username,
            "bio" to author.bio,
            "image" to author.image,
            "following" to false // @TODO
        ),
        "favorited" to (db.users.query().filter {
            (User::username eq call.getLoggedUserNameOrNull()) and (User::favorites contains article._id)
        }.count() != 0L)
    )
}

/*
suspend fun test() {
    val socket = aSocket().tcp().connect()
    val data = socket.openReadChannel().readPacket(4).readInt()
}
*/

fun main(args: Array<String>) {
    val ready = CompletableDeferred<Unit>()
    async {
        val server = aSocket().tcp().bind(InetSocketAddress(12000))
        while (true) {
            println("accept")
            ready.complete(Unit)
            val socket = server.accept()
            launch {
                val rc = socket.openReadChannel()
                val wc = socket.openWriteChannel(autoFlush = true)

                var seq = 0
                while (true) {
                    wc.writeByte(1)
                    wc.writeInt(1)
                }
            }
        }
    }

    runBlocking {
        ready.await()

        aSocket().tcp().connect(InetSocketAddress("localhost", 12000)).use { socket ->
            val rc = socket.openReadChannel()
            val wc = socket.openWriteChannel(autoFlush = true)
            while (true) {
                check(rc.readByte() == 1.toByte()) { "byte" }
                check(rc.readInt() == 1) { "int" }
                check(rc.readPacket(4).readInt() == 1) { "int" }
            }
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

