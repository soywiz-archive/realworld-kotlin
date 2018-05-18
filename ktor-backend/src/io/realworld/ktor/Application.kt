package io.realworld.ktor

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
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

open class MyJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
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
        val jwt = MyJWT(
            secret = environment.config.property("jwt.secret").getString()
        )
        mainModule(db, jwt)
    }
}

fun Application.mainModule(db: Db, jwt: MyJWT) {
    runBlocking {
        //install(HeadRequestSupport)
        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)
            header(HttpHeaders.Authorization)
            allowCredentials = true
            anyHost()
        }
        // @TODO: Implement this as a feature
        intercept(ApplicationCallPipeline.Infrastructure) {
            if (call.request.httpMethod == HttpMethod.Options) {
                //println("OPTIONS REQUEST")
                call.response.header("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, PATCH")
                call.respondText("")
                finish()
            } else {
                //println("OTHER REQUEST: ${call.request.httpMethod}")
                proceed()
            }
        }

        install(ContentNegotiation) {
            jackson {
            }
        }
        install(Authentication) {
            jwt {
                //schemes("Token", "Bearer")
                verifier(jwt.verifier)
                validate {
                    UserIdPrincipal(it.payload.getClaim("name").asString())
                }
            }
        }
        install(StatusPages) {
            exception<HttpStatusException> { cause ->
                call.respond(cause.status, cause.message ?: "error")
            }
        }

        routing {
            routeAuth(db, jwt)
            routeProfiles(db)
            routeArticleComments(db)
            routeArticles(db)
            routeTags(db)
            get("/") {
                call.respond(mapOf("ok" to "ok"))
            }
        }
    }
}