package xyz.mf7.kotlinpoet.sdk

import kotlin.reflect.KProperty

//inline fun <reified FROM, reified TO> pulumiFromJavaToKotlin(from: FROM): TO {
//    return null
//}


fun <T1> isPulumiType(clazz: Class<T1>): Boolean {
    return clazz.packageName.contains("pulumi")
}



fun <FROM, TO> pulumiArgsFromKotlinToJava(from: FROM & Any, to: Class<TO>): TO {
    val kotlinProperties = from::class.members.filterIsInstance<KProperty<*>>()

    val toBuilder = to.getMethod("builder").invoke(null)
    val builderMethods = toBuilder.javaClass.methods
        .filter { it.returnType.name.endsWith("Builder") }

    val propertyNameToValue = kotlinProperties.associate { it.name to it }
    builderMethods
        .filterNot { it.parameterTypes.any { type -> type.name.contains("Output") } || it.isVarArgs }
        .forEach { builderMethod ->
        propertyNameToValue.get(builderMethod.name) ?. let {
            val getterValue = it.getter.call(from)
            val valueToSet = if(getterValue == null) {
                null
            }
            else if(isPulumiType(getterValue.javaClass)) {
                pulumiArgsFromKotlinToJava(getterValue, builderMethod.parameters.get(0).type)
            }
            else {
                getterValue
            }

            builderMethod.invoke(toBuilder, valueToSet)
        }
    }

    val result = toBuilder.javaClass.getMethod("build").invoke(toBuilder)

    return result as TO
}