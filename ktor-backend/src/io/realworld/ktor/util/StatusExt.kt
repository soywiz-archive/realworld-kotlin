package io.realworld.ktor.util

import io.ktor.http.*

class HttpStatusException(val status: HttpStatusCode) : RuntimeException()

fun unauthorized(): Nothing = throw HttpStatusException(HttpStatusCode.Unauthorized)
fun notFound(): Nothing = throw HttpStatusException(HttpStatusCode.NotFound)