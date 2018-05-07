package io.realworld.ktor.route

import io.ktor.application.*
import io.ktor.auth.*

fun ApplicationCall.getLoggedUserName() = authentication.principal<UserIdPrincipal>()!!.name

