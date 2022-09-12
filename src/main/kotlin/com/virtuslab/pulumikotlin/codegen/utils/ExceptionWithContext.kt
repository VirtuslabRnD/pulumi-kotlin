package com.virtuslab.pulumikotlin.codegen.utils

class ExceptionWithContext(
    private val context: Map<String, Any?>,
    private val causedBy: Throwable? = null
): Exception("Exception context: ${contextToString(context)}", causedBy) {
    companion object {
        fun contextToString(context: Map<String, Any?>): String {
            return context.toList().joinToString(",") { (key, value) -> "$key->$value" }
        }
    }
}
