package org.virtuslab.pulumikotlin.codegen.step2intermediate

internal class SetWithKeyTransformer<K> private constructor(
    private val baseSet: Set<K>,
    private val keyTransformer: (K) -> K,
) : Set<K> by baseSet {

    override fun containsAll(elements: Collection<K>) =
        baseSet.containsAll(elements.map { keyTransformer(it) })

    override fun contains(element: K) =
        baseSet.contains(keyTransformer(element))

    override fun toString(): String {
        return "SetWithKeyTransformer(baseSet=$baseSet, keyTransformer=...)"
    }

    companion object {
        fun <K> from(baseSet: Set<K>, keyTransformer: (K) -> K): SetWithKeyTransformer<K> {
            val transformedSet = baseSet.map(keyTransformer).toSet()
            return SetWithKeyTransformer(transformedSet, keyTransformer)
        }
    }
}

internal fun <K> Set<K>.transformKeys(keyTransformer: (K) -> K) =
    SetWithKeyTransformer.from(this, keyTransformer)
