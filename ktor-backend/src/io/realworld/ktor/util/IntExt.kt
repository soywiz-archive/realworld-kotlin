package io.realworld.ktor.util

fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
