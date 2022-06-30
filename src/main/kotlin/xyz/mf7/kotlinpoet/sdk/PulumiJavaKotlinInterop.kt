package xyz.mf7.kotlinpoet.sdk

import java.util.*
import kotlin.reflect.KProperty

/**
 * this is disgusting but might work for the PoC
 */


fun <T1> isPulumiType(clazz: Class<T1>): Boolean {
    return clazz.packageName.contains("pulumi")
}


fun pulumiArgsFromJavaToKotlin(fromToRegistry: Map<Class<*>, Class<*>>, from: Any?): Any? {
    if(from == null) return null

    when(from) {
        is Number, is String, is Char, is Boolean -> return from

        is Map<*, *> -> return from.map { (k, v) ->
            pulumiArgsFromJavaToKotlin(fromToRegistry, k) to pulumiArgsFromJavaToKotlin(fromToRegistry, v)
        }

        is Array<*> -> return from.map { v ->
            pulumiArgsFromJavaToKotlin(fromToRegistry, v)
        }.toTypedArray()

        is List<*> -> return from.map { v ->
            pulumiArgsFromJavaToKotlin(fromToRegistry, v)
        }

        is Optional<*> -> return from.map { pulumiArgsFromJavaToKotlin(fromToRegistry, it) }.orElse(null)
    }

    val javaGetterMethods = from::class.java.methods.filter { it.parameterCount == 0 }

    val nameToMethod = javaGetterMethods.associateBy { it.name }

    val to = fromToRegistry[from.javaClass] ?:
    error("could not find for ${from.javaClass.name}")

    val toConstructor = to.kotlin.constructors.first()
    val constructorArgsByKParameter = toConstructor.parameters.associate {
        val result = nameToMethod[it.name!!]!!.invoke(from)
        val goodResult = pulumiArgsFromJavaToKotlin(fromToRegistry, result)

        it to goodResult
    }

    return toConstructor.callBy(constructorArgsByKParameter)
}

fun pulumiArgsFromKotlinToJava(fromToRegistry: Map<Class<*>, Class<*>>, from: Any?): Any? {

    if(from == null) return null

    when(from) {
        is Number, String, Char, Boolean -> return from
    }
    if(from::class.java.isPrimitive || from::class.java == String::class.java) return from

    if(Map::class.java.isInstance(from)) {
        return (from as Map<*, *>).map { (k, v) ->
            pulumiArgsFromKotlinToJava(fromToRegistry, k) to pulumiArgsFromKotlinToJava(fromToRegistry, v)
        }
    }

    if(Array::class.java.isInstance(from)) {
        return (from as Array<*>).map { v ->
            pulumiArgsFromKotlinToJava(fromToRegistry, v)
        }.toTypedArray()
    }

    if(List::class.java.isInstance(from)) {
        return (from as List<*>).map { v ->
            pulumiArgsFromKotlinToJava(fromToRegistry, v)
        }
    }

    val kotlinProperties = from::class.members.filterIsInstance<KProperty<*>>()

    val to = fromToRegistry[from.javaClass] ?:
    error("could not find for ${from.javaClass.name}")

    val toBuilder = to.getMethod("builder").invoke(null)
    val builderMethods = toBuilder.javaClass.methods
        .filter { it.returnType.name.endsWith("Builder") }

    val propertyNameToValue = kotlinProperties.associate { it.name to it }
    builderMethods
        .filterNot { it.parameterTypes.any { type -> type.name.contains("Output") } || it.isVarArgs }
        .forEach { builderMethod ->
        propertyNameToValue.get(builderMethod.name) ?. let {
            val getterValue = it.getter.call(from)
            val valueToSet = pulumiArgsFromKotlinToJava(fromToRegistry, getterValue)
            builderMethod.invoke(toBuilder, valueToSet)
        }
    }

    val result = toBuilder.javaClass.getMethod("build").invoke(toBuilder)

    return result
}