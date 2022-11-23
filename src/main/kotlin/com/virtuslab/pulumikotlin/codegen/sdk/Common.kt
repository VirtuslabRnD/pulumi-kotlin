// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch", "unused")

package com.pulumi.kotlin

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.pulumi.Context
import com.pulumi.core.Either
import com.pulumi.core.Output
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Optional
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource

@DslMarker
annotation class PulumiTagMarker

// TODO: make sure these helpers do not leak to the SDK

fun <T> List<ConvertibleToJava<T>>.toJava(): List<T> {
    return map { it.toJava() }
}

fun <T, T2> Map<T, ConvertibleToJava<T2>>.toJava(): Map<T, T2> {
    return map { (key, value) -> key to value.toJava() }.toMap()
}

fun <T> Output<out ConvertibleToJava<T>>.toJava(): Output<T> {
    return applyValue { it.toJava() }
}

@JvmName("A0E1B7D29")
fun <T1, T2> Output<out Map<T1, ConvertibleToJava<T2>>>.toJava(): Output<Map<T1, T2>> {
    return applyValue { it.toJava() }
}

@JvmName("AB82F3249")
fun <T> Output<out List<ConvertibleToJava<T>>>.toJava(): Output<List<T>> {
    return applyValue { it.toJava() }
}

@JvmName("A500F3FFF")
fun <T> Output<T>.toJava(): Output<T> {
    return this
}

@JvmName("A500F3FFG")
fun Output<out List<KotlinResource>>.toJava(): Output<List<JavaResource>> {
    return applyValue { listOfKotlinResources -> listOfKotlinResources.map { it.javaResource } }
}

@JvmName("A1E842B23")
fun <T> List<T>.toJava(): List<T> {
    return this
}

@JvmName("A3F01FCF1")
fun <T1, T2> Map<T1, T2>.toJava(): Map<T1, T2> {
    return this
}

@JvmName("D26N8RBZK")
fun <T1, T2, T3> Map<T1, Either<out ConvertibleToJava<T2>, T3>>.toJava(): Map<T1, Either<T2, T3>> {
    return map { (key, value) -> key to value.toJava() }.toMap()
}

@JvmName("QUMXW7VS4")
fun <T1, T2, T3> Output<out Map<T1, Either<out ConvertibleToJava<T2>, T3>>>.toJava():
    Output<Map<T1, Either<T2, T3>>> {
    return applyValue { it.map { (key, value) -> key to value.toJava() }.toMap() }
}

@JvmName("A3F01FCF2")
fun <T, R> Output<out Either<T, out ConvertibleToJava<R>>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it }, { it.toJava() }) }
}

@JvmName("A3F01FCF3")
fun <T, R> Output<out Either<out ConvertibleToJava<T>, out ConvertibleToJava<R>>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it.toJava() }, { it.toJava() }) }
}

@JvmName("A3F01FCF4")
fun <T, R> Output<out Either<out ConvertibleToJava<T>, R>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it.toJava() }, { it }) }
}

@JvmName("E30Q7HN7N")
fun <T, R> Either<T, R>.toJava(): Either<T, R> {
    return this
}

@JvmName("PI4C93O0J")
fun <T, R> Either<out ConvertibleToJava<T>, R>.toJava(): Either<T, R> {
    return applyValue { either -> either.transform({ it.toJava() }, { it }) }
}

@JvmName("ZW1GDUNN4")
fun <T, R> Either<T, out ConvertibleToJava<R>>.toJava(): Either<T, R> {
    return applyValue { either -> either.transform({ it }, { it.toJava() }) }
}

@JvmName("K9D35N5P0")
fun Either<String, List<String>>.toJava(): Either<String, List<String>> {
    return this
}

fun kotlinx.serialization.json.JsonElement.toJava(): com.google.gson.JsonElement {
    return JsonParser.parseString(Json.encodeToString(this))
}

@JvmName("GYYIYQY07")
fun List<kotlinx.serialization.json.JsonElement>.toJava(): List<com.google.gson.JsonElement> {
    return map { it.toJava() }
}

@JvmName("XYRS71481")
fun Output<kotlinx.serialization.json.JsonElement>.toJava(): Output<com.google.gson.JsonElement> {
    return applyValue { it.toJava() }
}

@JvmName("MJX738L6N")
fun Output<out List<kotlinx.serialization.json.JsonElement>>.toJava(): Output<List<com.google.gson.JsonElement>> {
    return applyValue { list -> list.map { it.toJava() } }
}

@JvmName("5JP74J66L")
fun <T> Either<T, out List<kotlinx.serialization.json.JsonElement>>.toJava():
    Either<T, List<com.google.gson.JsonElement>> {
    return transform({ it }, { list -> list.map { it.toJava() } })
}

@JvmName("QW91S6Y8Z")
fun <T> Either<out ConvertibleToJava<T>, out List<kotlinx.serialization.json.JsonElement>>.toJava():
    Either<T, List<com.google.gson.JsonElement>> {
    return transform({ it.toJava() }, { list -> list.map { it.toJava() } })
}

@JvmName("DRBQ7WHL7")
fun <T> Output<out Either<out ConvertibleToJava<T>, List<kotlinx.serialization.json.JsonElement>>>.toJava():
    Output<Either<T, List<com.google.gson.JsonElement>>> {
    return applyValue { either -> either.transform({ it.toJava() }, { list -> list.map { it.toJava() } }) }
}

fun com.google.gson.JsonElement.toKotlin(): kotlinx.serialization.json.JsonElement {
    return Json.parseToJsonElement(GsonBuilder().serializeNulls().create().toJson(this))
}

fun List<com.google.gson.JsonElement>.toKotlin(): List<kotlinx.serialization.json.JsonElement> {
    return map { it.toKotlin() }
}

@JvmName("KGD40RD3Y")
fun Output<com.google.gson.JsonElement>.toKotlin(): Output<kotlinx.serialization.json.JsonElement> {
    return applyValue { it.toKotlin() }
}

fun Optional<com.google.gson.JsonElement>.toKotlin(): kotlinx.serialization.json.JsonElement? {
    return map { it.toKotlin() }.orElse(null)
}

@JvmName("YU8JE7M1A")
fun Optional<Output<com.google.gson.JsonElement>>.toKotlin(): Output<kotlinx.serialization.json.JsonElement> {
    return map { it.toKotlin() }.orElse(null)
}

@JvmName("D2L1K8JJW")
fun Optional<Output<List<com.google.gson.JsonElement>>>.toKotlin():
    Output<List<kotlinx.serialization.json.JsonElement>> {
    return map { output -> output.applyValue { it.toKotlin() } }.orElse(null)
}

@JvmName("17UZ2IEB7")
fun <T> Either<T, out List<com.google.gson.JsonElement>>.toKotlin():
    Either<T, List<kotlinx.serialization.json.JsonElement>> {
    return transform({ it.toKotlin() }, { it.toKotlin() })
}

@JvmName("AXJTOUDDQ")
fun <T> Optional<Either<T, List<com.google.gson.JsonElement>>>.toKotlin():
    Either<T, List<kotlinx.serialization.json.JsonElement>> {
    return map { it.toKotlin() }.orElse(null)
}

@JvmName("2CRULMM2Y")
fun <T> Optional<out Output<out Either<T, out List<com.google.gson.JsonElement>>>>.toKotlin():
    Output<Either<T, List<kotlinx.serialization.json.JsonElement>>> {
    return map { output ->
        output.applyValue { either ->
            either.transform({ it.toKotlin() }, { it.toKotlin() })
        }
    }
        .orElse(null)
}

fun <T> Optional<T>.toKotlin(): T? {
    return orElse(null)
}

fun <T> T.toKotlin(): T {
    return this
}

fun <T, R> T.applyValue(f: (T) -> R): R {
    return f(this)
}

@Suppress("RedundantSuspendModifier")
suspend inline fun <T> T.applySuspend(block: T.() -> Unit): T {
    block()
    return this
}

interface ConvertibleToJava<T> {
    fun toJava(): T
}

/**
 * Parent class for resources within Kotlin SDK - equivalent to [JavaResource].
 *
 * Each resource within Kotlin SDK should have corresponding [ResourceMapper],
 * in order to properly translate Java resources to Kotlin representation.
 *
 * This class serves only as parent for all resources and should not be instantiated,
 * it cannot be sealed, because generated subclasses will be placed in other packages.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinResource
private constructor(internal open val javaResource: JavaResource) {
    protected constructor(
        javaResource: JavaResource,
        mapper: ResourceMapper<KotlinResource>,
    ) : this(javaResource) {
        GlobalResourceMapper.registerMapper(mapper)
    }
}

/**
 * Parent class for provider resources within Kotlin SDK - equivalent to [JavaProviderResource].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinProviderResource protected constructor(
    override val javaResource: JavaProviderResource,
    mapper: ResourceMapper<KotlinResource>,
) : KotlinResource(javaResource, mapper)

object Pulumi {

    /**
     * Run a Pulumi stack callback and wait for result.
     * In case of an error terminates the process with [System.exit].
     *
     * @param block the stack to run in Pulumi runtime
     */
    fun run(block: suspend (Context) -> Unit) {
        com.pulumi.Pulumi.run {
            runBlocking {
                block(it)
            }
        }
    }
}
