package io.realworld.ktor.util

import io.ktor.experimental.client.mongodb.bson.*
import kotlin.reflect.*

interface Extra {
    val extra: ExtraData

    class Mixin(override val extra: ExtraData = ExtraData()) :
        Extra {
        constructor(data: BsonDocument) : this(ExtraData(data))
    }
}

typealias ExtraData = LinkedHashMap<String, Any?>

class ExtraProperty<T>(val propName: String? = null, val gen: () -> T) {
    operator fun setValue(extra: Extra, property: KProperty<*>, value: T) {
        extra.extra[propName ?: property.name] = value
    }

    operator fun getValue(extra: Extra, property: KProperty<*>): T {
        return (extra.extra as MutableMap<String, T>).getOrPut(propName ?: property.name) { gen() }
    }
}

fun <T> Extra(propName: String? = null, gen: () -> T) = ExtraProperty(propName, gen)