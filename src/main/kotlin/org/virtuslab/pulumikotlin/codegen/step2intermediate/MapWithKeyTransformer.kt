package org.virtuslab.pulumikotlin.codegen.step2intermediate

import mu.KotlinLogging
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MapWithKeyTransformer.ConflictStrategy
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MapWithKeyTransformer.ConflictStrategy.Companion.failOnConflicts

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
        fun <K, V : Any> from(
            baseMap: Map<K, V>,
            keyTransformer: (K) -> K,
            conflictStrategy: ConflictStrategy<K, V> = failOnConflicts(),
        ): MapWithKeyTransformer<K, V> {
            val transformedMap = mutableMapOf<K, V>()
            baseMap.forEach { (key, value) ->
                val transformedKey = keyTransformer(key)
                transformedMap.merge(transformedKey, value) { oldValue, newValue ->
                    conflictStrategy.resolveConflict(
                        newKey = transformedKey,
                        newValue = newValue,
                        oldKey = key,
                        oldValue = oldValue,
                    )
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

    fun interface ConflictStrategy<K, V> {
        fun resolveConflict(newKey: K, newValue: V, oldKey: K, oldValue: V): V

        companion object {
            private val logger = KotlinLogging.logger {}

            fun <K, V> failOnConflicts() = ConflictStrategy { newKey: K, newValue: V, oldKey: K, oldValue: V ->
                if (oldValue === newValue) {
                    newValue
                } else if (oldValue == newValue) {
                    logger.warn(
                        "Found conflicting keys ($oldKey, $newKey), but values are the same ($oldValue, $newValue).",
                    )
                    newValue
                } else {
                    throw ConflictsNotAllowed.from(oldKey, newKey)
                }
            }

            fun <K, V> mergeSetsOnConflicts() = ConflictStrategy<K, Set<V>> { _, newValue, _, oldValue ->
                oldValue + newValue
            }
        }
    }
}

internal fun <V : Any> Map<String, V>.lowercaseKeys(conflictStrategy: ConflictStrategy<String, V> = failOnConflicts()) =
    transformKeys(conflictStrategy) { it.lowercase() }

internal fun <K, V : Any> Map<K, V>.transformKeys(
    conflictStrategy: ConflictStrategy<K, V> = failOnConflicts(),
    keyTransformer: (K) -> K,
) =
    MapWithKeyTransformer.from(this, keyTransformer, conflictStrategy)
