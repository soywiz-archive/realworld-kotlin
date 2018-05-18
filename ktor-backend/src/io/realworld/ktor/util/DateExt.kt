package io.realworld.ktor.util

import java.text.*
import java.util.*

fun Date.format(format: SimpleDateFormat) = format.format(this)
