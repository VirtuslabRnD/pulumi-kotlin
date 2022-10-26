package com.virtuslab.pulumikotlin.codegen.step2intermediate

internal class MapWithKeyTransformer<K, V> private constructor(
    private val baseMap: Map<K, V>,
    private val keyTransformer: (K) -> K,
) : Map<K, V> by baseMap {

    override fun containsKey(key: K): Boolean {
        return baseMap.containsKey(keyTransformer(key))
    }

    override operator fun get(key: K): V? {
        return baseMap[keyTransformer(key)]
    }

    companion object {
        fun <K, V : Any> from(baseMap: Map<K, V>, keyTransformer: (K) -> K): MapWithKeyTransformer<K, V> {
            val transformedMap = mutableMapOf<K, V>()
            baseMap.forEach { (key, value) ->
                val transformedKey = keyTransformer(key)
                transformedMap.merge(transformedKey, value) { oldValue, newValue ->
                    if (oldValue === newValue) {
                        newValue
                    } else if (oldValue == newValue) {
                        println(
                            "WARN: Found conflicting keys ($key, $transformedKey), " +
                                "but values are the same ($oldValue, $newValue).",
                        )
                        newValue
                    } else {
                        throw ConflictsNotAllowed.from(key, transformedKey)
                    }
                }
            }
            return MapWithKeyTransformer(transformedMap, keyTransformer)
        }
    }

    class ConflictsNotAllowed(
        keyBefore: String,
        keyAfter: String,
    ) : RuntimeException("Transformed key ($keyBefore -> $keyAfter) conflicts with an existing key ($keyAfter)") {
        companion object {
            fun <K> from(keyBefore: K, keyAfter: K) =
                ConflictsNotAllowed(keyBefore.toString(), keyAfter.toString())
        }
    }
}

internal fun <V : Any> Map<String, V>.lowercaseKeys() =
    transformKeys { it.lowercase() }

internal fun <K, V : Any> Map<K, V>.transformKeys(keyTransformer: (K) -> K) =
    MapWithKeyTransformer.from(this, keyTransformer)
