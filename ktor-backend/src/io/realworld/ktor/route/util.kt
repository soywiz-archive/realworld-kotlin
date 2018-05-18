package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*

fun ApplicationCall.getLoggedUserNameOrNull() = authentication.principal<UserIdPrincipal>()?.name
fun ApplicationCall.getLoggedUserName() = getLoggedUserNameOrNull() ?: error("User not logged")
