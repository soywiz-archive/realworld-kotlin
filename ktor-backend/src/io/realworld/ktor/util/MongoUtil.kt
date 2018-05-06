package io.realworld.ktor.util

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.*
import com.fasterxml.jackson.databind.ser.std.*
import com.soywiz.io.ktor.client.mongodb.*
import com.soywiz.io.ktor.client.mongodb.bson.*
import kotlin.reflect.*

@JsonSerialize(using = MongoEntitySerializer::class)
open class MongoEntity<T : MongoEntity<T>>(data: BsonDocument) : Extra by Extra.Mixin(
    data
) {
    var _id: BsonObjectId? by Extra { null }
    fun extract(vararg props: KProperty1<T, *>): BsonDocument {
        val out = LinkedHashMap<String, Any?>()
        for (prop in props) out[prop.name] = this.extra[prop.name]
        return out
    }

    override fun toString(): String = "${this::class.simpleName}($extra)"
}

class MongoEntitySerializer : StdSerializer<MongoEntity<*>>(MongoEntity::class.java) {
    override fun serialize(value: MongoEntity<*>, jgen: JsonGenerator, provider: SerializerProvider) {
        jgen.writeObject(value.extra.toMap() - MongoEntity<*>::_id.name)
    }
}

object JSON {
    val mapper = ObjectMapper()
    fun serialize(value: Any?): String = mapper.writeValueAsString(value)
}

fun Any?.toJson() = JSON.serialize(this)

suspend fun MongoDBCollection.ensureIndex(
    name: String,
    vararg keys: Pair<String, Int>,
    unique: Boolean? = null
): MongoDBCollection {
    try {
        createIndex(name, *keys, unique = unique)
    } catch (e: MongoDBException) {
        e.printStackTrace()
    }
    return this
}

class MongoDBTypedCollection<T : MongoEntity<T>>(val gen: (BsonDocument) -> T, val collection: MongoDBCollection) {
    suspend fun ensureIndex(
        vararg keys: Pair<KProperty1<T, *>, Int>,
        name: String? = null,
        unique: Boolean? = null
    ): MongoDBTypedCollection<T> {
        val rname = name ?: keys.joinToString("_") { it.first.name }+(if (unique == true) "_unique" else "")
        collection.ensureIndex(rname, *keys.map { it.first.name to it.second }.toTypedArray(), unique = unique)
        return this
    }

    suspend fun insert(vararg item: T) {
        val result = collection.insert(*item.map { it.extra }.toTypedArray())
        println(result)
    }

    inner class Expr {
        infix fun KProperty1<T, *>.eq(other: Any?) = mapOf(this.name to mapOf("\$eq" to other))
        infix fun Any?.and(other: Any?) = mapOf("\$and" to listOf(this, other))
    }

    private val expr = Expr()
    @Suppress("UNCHECKED_CAST")
    suspend fun find(query: Expr.() -> BsonDocument): List<T> {
        val cond = query(expr)
        //println(cond)
        val result = collection.find() { cond }
        //println(result)
        return result.map { gen(it) }
    }

    suspend fun findOneOrNull(query: Expr.() -> BsonDocument): T? = find(query).firstOrNull()
    suspend fun findOne(query: Expr.() -> BsonDocument): T = find(query).firstOrNull() ?: error("Can't find item")
    suspend fun update(item: T, vararg props: KMutableProperty1<T, *>) {
        collection.update(
            MongoUpdate(
                mapOf(
                    "\$set" to props.associate { it.name to item.extra[it.name] }
                )
            ) {
                "_id" eq item._id
            }
        )
    }
}

fun <T : MongoEntity<T>> MongoDBCollection.typed(gen: (BsonDocument) -> T): MongoDBTypedCollection<T> {
    return MongoDBTypedCollection(gen, this)
}

