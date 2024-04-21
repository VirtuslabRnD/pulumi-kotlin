package com.pulumi.kotlin

import com.pulumi.core.Tuples

/**
 * Extension for [com.pulumi.core.Tuples.Tuple1] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple1<T>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple2] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple2<T, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple2] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple2<*, T>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple3] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple3<T, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple3] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple3<*, T, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple3] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple3<*, *, T>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple4] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple4<T, *, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple4] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple4<*, T, *, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple4] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple4<*, *, T, *>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple4] to work with destructuring declarations.
 *
 * @param T the type of the fourth component.
 * @return The fourth component.
 */
operator fun <T> Tuples.Tuple4<*, *, *, T>.component4(): T? = t4

/**
 * Extension for [com.pulumi.core.Tuples.Tuple5] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple5<T, *, *, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple5] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple5<*, T, *, *, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple5] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple5<*, *, T, *, *>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple5] to work with destructuring declarations.
 *
 * @param T the type of the fourth component.
 * @return The fourth component.
 */
operator fun <T> Tuples.Tuple5<*, *, *, T, *>.component4(): T? = t4

/**
 * Extension for [com.pulumi.core.Tuples.Tuple5] to work with destructuring declarations.
 *
 * @param T the type of the fifth component.
 * @return The fifth component.
 */
operator fun <T> Tuples.Tuple5<*, *, *, *, T>.component5(): T? = t5

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple6<T, *, *, *, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple6<*, T, *, *, *, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple6<*, *, T, *, *, *>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the fourth component.
 * @return The fourth component.
 */
operator fun <T> Tuples.Tuple6<*, *, *, T, *, *>.component4(): T? = t4

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the fifth component.
 * @return The fifth component.
 */
operator fun <T> Tuples.Tuple6<*, *, *, *, T, *>.component5(): T? = t5

/**
 * Extension for [com.pulumi.core.Tuples.Tuple6] to work with destructuring declarations.
 *
 * @param T the type of the sixth component.
 * @return The sixth component.
 */
operator fun <T> Tuples.Tuple6<*, *, *, *, *, T>.component6(): T? = t6

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple7<T, *, *, *, *, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple7<*, T, *, *, *, *, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple7<*, *, T, *, *, *, *>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the fourth component.
 * @return The fourth component.
 */
operator fun <T> Tuples.Tuple7<*, *, *, T, *, *, *>.component4(): T? = t4

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the fifth component.
 * @return The fifth component.
 */
operator fun <T> Tuples.Tuple7<*, *, *, *, T, *, *>.component5(): T? = t5

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the sixth component.
 * @return The sixth component.
 */
operator fun <T> Tuples.Tuple7<*, *, *, *, *, T, *>.component6(): T? = t6

/**
 * Extension for [com.pulumi.core.Tuples.Tuple7] to work with destructuring declarations.
 *
 * @param T the type of the seventh component.
 * @return The seventh component.
 */
operator fun <T> Tuples.Tuple7<*, *, *, *, *, *, T>.component7(): T? = t7

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the first component.
 * @return The first component.
 */
operator fun <T> Tuples.Tuple8<T, *, *, *, *, *, *, *>.component1(): T? = t1

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the second component.
 * @return The second component.
 */
operator fun <T> Tuples.Tuple8<*, T, *, *, *, *, *, *>.component2(): T? = t2

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the third component.
 * @return The third component.
 */
operator fun <T> Tuples.Tuple8<*, *, T, *, *, *, *, *>.component3(): T? = t3

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the fourth component.
 * @return The fourth component.
 */
operator fun <T> Tuples.Tuple8<*, *, *, T, *, *, *, *>.component4(): T? = t4

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the fifth component.
 * @return The fifth component.
 */
operator fun <T> Tuples.Tuple8<*, *, *, *, T, *, *, *>.component5(): T? = t5

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the sixth component.
 * @return The sixth component.
 */
operator fun <T> Tuples.Tuple8<*, *, *, *, *, T, *, *>.component6(): T? = t6

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the seventh component.
 * @return The seventh component.
 */
operator fun <T> Tuples.Tuple8<*, *, *, *, *, *, T, *>.component7(): T? = t7

/**
 * Extension for [com.pulumi.core.Tuples.Tuple8] to work with destructuring declarations.
 *
 * @param T the type of the eighth component.
 * @return The eighth component.
 */
operator fun <T> Tuples.Tuple8<*, *, *, *, *, *, *, T>.component8(): T? = t8
