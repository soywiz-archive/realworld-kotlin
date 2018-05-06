package io.realworld.ktor.util

class UnauthorizedException : RuntimeException()

fun unauthorized(): Nothing = throw UnauthorizedException()