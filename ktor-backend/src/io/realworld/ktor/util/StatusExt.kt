package io.realworld.ktor.util

import io.ktor.http.*

class HttpStatusException(val status: HttpStatusCode, message: String) : RuntimeException(message)

fun unauthorized(msg: String = "unauthorized"): Nothing = throw HttpStatusException(HttpStatusCode.Unauthorized, msg)
fun notFound(msg: String = "not found"): Nothing = throw HttpStatusException(HttpStatusCode.NotFound, msg)
fun methodNotAllowed(msg: String = "method not allowed"): Nothing =
    throw HttpStatusException(HttpStatusCode.MethodNotAllowed, msg)