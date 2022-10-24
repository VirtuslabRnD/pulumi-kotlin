package com.virtuslab.pulumikotlin.codegen.step2intermediate

internal class SetWithKeyTransformer<K> private constructor(
    private val baseSet: Set<K>,
    private val keyTransformer: (K) -> K,
) : Set<K> by baseSet {

    override fun containsAll(elements: Collection<K>) =
        baseSet.containsAll(elements.map { keyTransformer(it) })

    override fun contains(element: K) =
        baseSet.contains(keyTransformer(element))

    companion object {
        fun <K> from(baseSet: Set<K>, keyTransformer: (K) -> K): SetWithKeyTransformer<K> {
            val transformedSet = mutableSetOf<K>()
            baseSet.forEach { key ->
                val transformedKey = keyTransformer(key)
                if (transformedSet.contains(transformedKey)) {
                    throw ConflictsNotAllowed.from(key, transformedKey)
                }
                transformedSet.add(transformedKey)
            }
            return SetWithKeyTransformer(transformedSet, keyTransformer)
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

internal fun <K> Set<K>.transformKeys(keyTransformer: (K) -> K) =
    SetWithKeyTransformer.from(this, keyTransformer)
