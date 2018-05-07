package io.realworld.ktor

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.jackson.*
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
        val myjwt = MyJWT(
            secret = environment.config.property("jwt.secret").getString()
        )

        install(ContentNegotiation) {
            jackson {
            }
        }
        install(Authentication) {
            jwt {
                verifier(myjwt.verifier)
                validate {
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
            routeAuth(db, myjwt)
            routeArticles(db)
            routeTags(db)
            get("/") {
                call.respond(mapOf("ok" to "ok"))
            }
        }
    }
}
