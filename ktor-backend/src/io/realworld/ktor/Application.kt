package io.realworld.ktor

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.soywiz.io.ktor.client.mongodb.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.realworld.ktor.model.*
import io.realworld.ktor.route.*
import io.realworld.ktor.util.*
import kotlinx.coroutines.experimental.*

fun main(args: Array<String>): Unit {

    //val user = User().apply {
    //    email = "test"
    //}

    //println(user)

    io.ktor.server.netty.DevelopmentEngine.main(args)
}

object MyJWT {
    private val secret = "mysupersecret"
    private val audience = "conduit-server"
    private val issuer = "http://127.0.0.1/"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun sign(name: String): String = JWT.create()
        .withClaim("name", name)
        .withAudience(audience)
        .withIssuer(issuer)
        .sign(algorithm)
}


/**
 * https://www.getpostman.com/
 * https://github.com/gothinkster/realworld/blob/master/api/Conduit.postman_collection.json
 *
 * https://editor.swagger.io/
 * https://raw.githubusercontent.com/gothinkster/realworld/master/api/swagger.json
 */
@Suppress("unused")
fun Application.main() {
    runBlocking {
        val db = Db()

        install(ContentNegotiation) {
            jackson {
            }
        }
        install(Authentication) {
            this.jwt {
                this.verifier(MyJWT.verifier)
                this.validate {
                    UserIdPrincipal(it.payload.getClaim("name").asString())
                }
            }
        }
        install(StatusPages) {
            exception<HttpStatusException> { cause ->
                call.respond(cause.status)
            }
        }

        routing {
            routeAuth(db)
            routeArticles(db)
            routeTags(db)
            get("/") {
                call.respond(mapOf("ok" to "ok"))
            }
        }
    }
}
