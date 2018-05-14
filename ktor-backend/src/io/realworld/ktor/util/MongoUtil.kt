package io.realworld.ktor.util

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.*
import com.fasterxml.jackson.databind.ser.std.*
import com.soywiz.io.ktor.client.mongodb.*
import com.soywiz.io.ktor.client.mongodb.bson.*
import com.soywiz.io.ktor.client.util.*
import com.soywiz.io.ktor.client.util.sync.*
import java.io.*
import java.net.*
import java.security.*
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

    fun extractAll(): BsonDocument = extra.toMap()
    fun extractAllBut(vararg props: KProperty1<T, *>): BsonDocument = extra.toMap() - props.map { it.name }

    fun ensureNotNull(vararg props: KProperty1<T, *>): T = (this as T).apply {
        for (prop in props) {
            if (extra[prop.name] == null) throw kotlin.NullPointerException()
        }
    }

    override fun toString(): String = "${this::class.simpleName}($extra)"
}

class MongoEntitySerializer : StdSerializer<MongoEntity<*>>(MongoEntity::class.java) {
    override fun serialize(value: MongoEntity<*>, jgen: JsonGenerator, provider: SerializerProvider) {
        jgen.writeObject(value.extra.toMutableMap().apply { this.remove(MongoEntity<*>::_id.name) })
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

    suspend fun insert(
        vararg item: T,
        ordered: Boolean? = null,
        writeConcern: BsonDocument? = null,
        bypassDocumentValidation: Boolean? = null
    ): List<T> {
        for (i in item) {
            if (i._id == null) i._id = MongoObjectIdGenerator.generate()
        }
        val result = collection.insert(
            *item.map { it.extra }.toTypedArray(),
            ordered = ordered,
            writeConcern = writeConcern,
            bypassDocumentValidation = bypassDocumentValidation
        )
        println(result)
        return item.toList()
    }

    /**
     * https://docs.mongodb.com/manual/reference/operator/query/
     */
    inner class Expr {
        fun all(): BsonDocument = mapOf()

        infix fun KProperty1<T, *>.eq(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$eq" to value))
        infix fun KProperty1<T, *>.ne(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$ne" to value))
        infix fun KProperty1<T, *>.gt(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$gt" to value))
        infix fun KProperty1<T, *>.gte(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$gte" to value))
        infix fun KProperty1<T, *>.lt(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$lt" to value))
        infix fun KProperty1<T, *>.lte(value: Any?): BsonDocument = mapOf(this.name to mapOf("\$lte" to value))
        infix fun KProperty1<T, *>._in(value: List<Any?>?): BsonDocument = mapOf(this.name to mapOf("\$in" to value))
        infix fun KProperty1<T, *>._nin(value: List<Any?>?): BsonDocument = mapOf(this.name to mapOf("\$nin" to value))

        fun and(vararg items: BsonDocument): BsonDocument = mapOf("\$and" to items.toList())
        fun or(vararg items: BsonDocument): BsonDocument = mapOf("\$or" to items.toList())
        fun nor(vararg items: BsonDocument): BsonDocument = mapOf("\$nor" to items.toList())

        infix fun BsonDocument.and(other: BsonDocument): BsonDocument = and(this, other)
        infix fun BsonDocument.or(other: BsonDocument): BsonDocument = or(this, other)
        infix fun BsonDocument.nor(other: BsonDocument): BsonDocument = nor(this, other)
        fun BsonDocument.not(): BsonDocument = mapOf("\$not" to this)

        fun KProperty1<T, *>.exists(exists: Boolean = true) = mapOf(this.name to mapOf("\$exists" to exists))
        fun KProperty1<T, *>.eqType(type: String) = mapOf(this.name to mapOf("\$type" to type))

        // @TODO: Add to untyped too
        infix fun KProperty1<T, *>.regex(value: Regex): BsonDocument = mapOf(this.name to mapOf("\$regex" to value))

        infix fun <T2> KProperty1<T, List<T2>?>.contains(value: T2): BsonDocument =
            mapOf(this.name to mapOf("\$all" to listOf(value)))

        infix fun <T2> KProperty1<T, List<T2>?>.all(value: List<T2>): BsonDocument =
            mapOf(this.name to mapOf("\$all" to value))

        infix fun KProperty1<T, List<*>?>.elemMatch(cond: BsonDocument): BsonDocument =
            mapOf(this.name to mapOf("\$elemMatch" to cond))

        infix fun KProperty1<T, List<*>?>.size(count: Int): BsonDocument = mapOf(this.name to mapOf("\$size" to count))
    }

    private val expr = Expr()
    @Suppress("UNCHECKED_CAST")
    // How nice would be to have LINQ from C#?
    // https://msdn.microsoft.com/en-us/library/system.linq.expressions(v=vs.110).aspx
    suspend fun find(query: Expr.() -> BsonDocument = { all() }): List<T> {
        val cond = query(expr)
        //println(cond)
        val result = collection.find { cond }
        //println(result)
        return result.map { gen(it) }
    }

    suspend fun delete(query: Expr.() -> BsonDocument = { all() }) {
        val result = collection.delete(false) { query(expr) }
        println(result)
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

    suspend fun <R> updatePush(item: T, prop: KProperty1<T, List<R?>?>, valueToPush: R, once: Boolean = false) {
        collection.update(
            MongoUpdate(
                mapOf(
                    (if (once) "\$addToSet" else "\$push") to mapOf(prop.name to valueToPush)
                )
            ) {
                "_id" eq item._id
            }
        )
    }

    suspend fun <R> updatePull(item: T, prop: KProperty1<T, List<R?>?>, valueToPull: R) {
        collection.update(
            MongoUpdate(
                mapOf(
                    "\$pull" to mapOf(prop.name to valueToPull)
                )
            ) {
                "_id" eq item._id
            }
        )
    }

    suspend fun <R> distinct(
        field: KProperty1<T, List<R?>?>,
        readConcern: BsonDocument? = null,
        collation: BsonDocument? = null,
        query: Expr.() -> BsonDocument = { all() }
    ): List<R> = distinct(field, readConcern, collation, false, query) as List<R>

    /**
     * https://docs.mongodb.com/v3.4/reference/command/distinct/
     */
    suspend fun distinct(
        field: KProperty1<T, *>,
        readConcern: BsonDocument? = null,
        collation: BsonDocument? = null,
        @Suppress("UNUSED_PARAMETER") dummy: Boolean = false,
        query: Expr.() -> BsonDocument = { all() }
    ): List<*> {
        // Reply(packet=MongoPacket(opcode=1, requestId=93, responseTo=2, payload=SIZE(191)), responseFlags=8, cursorID=0, startingFrom=0,
        // documents=[{waitedMS=0, values=[dragons, training], stats={n=2, nscanned=0, nscannedObjects=2, timems=0, planSummary=COLLSCAN}, ok=1.0}])
        //{
        //    waitedMS = 0,
        //    values = [dragons, training],
        //    stats = {
        //        n = 2,
        //        nscanned = 0,
        //        nscannedObjects = 2,
        //        timems = 0,
        //        planSummary = COLLSCAN
        //    },
        //    ok = 1.0
        //}
        val result = collection.db.runCommand {
            putNotNull("distinct", collection.collection)
            putNotNull("key", field.name)
            putNotNull("query", query(expr))
            putNotNull("readConcern", readConcern)
            putNotNull("collation", collation)
        }.checkErrors()
        return Dynamic { result.firstDocument["values"].list }
    }
}

fun <T : MongoEntity<T>> MongoDBCollection.typed(gen: (BsonDocument) -> T): MongoDBTypedCollection<T> {
    return MongoDBTypedCollection(gen, this)
}

// https://docs.mongodb.com/manual/reference/method/ObjectId/#ObjectId
// a 4-byte value representing the seconds since the Unix epoch,
// a 3-byte machine identifier,
// a 2-byte process id, and
// a 3-byte counter, starting with a random value.

// A globally unique identifier for objects.
// Consists of 12 bytes, divided as follows:
// ObjectID layout
// 0123 456     78   91011
// time machine pid  inc
// Note that the numbers are stored in big-endian order.
object MongoObjectIdGenerator {
    private val machineId by lazy { createMachineIdentifier() }
    private val processId by lazy { createProcessIdentifier() }
    private var counter = SecureRandom().nextInt() and 0xFFFFFF

    fun generate(): BsonObjectId = synchronized(this) {
        return BsonObjectId(ByteArrayOutputStream().apply {
            write32_be((System.currentTimeMillis() / 1000L).toInt())
            write24_be(machineId)
            write16_be(processId)
            write24_be(counter++)
        }.toByteArray())
    }

    private fun createProcessIdentifier(): Int {
        return try {
            val mxName = java.lang.management.ManagementFactory.getRuntimeMXBean().name
            return mxName.split("@").firstOrNull()?.toIntOrNull() ?: mxName.hashCode()
        } catch (t: Throwable) {
            SecureRandom().nextInt()
        }
    }

    private fun createMachineIdentifier(): Int = try {
        var sb = 0
        for (ni in NetworkInterface.getNetworkInterfaces()) {
            sb += ni.hashCode()
            val mac = ni.hardwareAddress
            if (mac != null) for (m in mac) sb += m.toInt()
        }
        sb
    } catch (t: Throwable) {
        SecureRandom().nextInt()
    } and 0xFFFFFF
}

val BsonObjectId.hex: String get() = Hex.encode(this.data)