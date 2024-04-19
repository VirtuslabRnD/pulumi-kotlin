package com.pulumi.kotlin

import com.pulumi.core.Tuples
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TuplesTest {

    @Test
    fun `can destruct Tuple1`() {
        val tuple = Tuples.Tuple1(1.toByte())
        val (value1) = tuple
        assertEquals(1.toByte(), value1)
    }

    @Test
    fun `can destruct Tuple2`() {
        val tuple = Tuples.Tuple2(1.toByte(), 2)
        val (value1, value2) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
    }

    @Test
    fun `can destruct Tuple3`() {
        val tuple = Tuples.Tuple3(1.toByte(), 2, 3L)
        val (value1, value2, value3) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
    }

    @Test
    fun `can destruct Tuple4`() {
        val tuple = Tuples.Tuple4(1.toByte(), 2, 3L, 4.0f)
        val (value1, value2, value3, value4) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
        assertEquals(4.0f, value4)
    }

    @Test
    fun `can destruct Tuple5`() {
        val tuple = Tuples.Tuple5(1.toByte(), 2, 3L, 4.0f, 5.0)
        val (value1, value2, value3, value4, value5) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
        assertEquals(4.0f, value4)
        assertEquals(5.0, value5)
    }

    @Test
    fun `can destruct Tuple6`() {
        val tuple = Tuples.Tuple6(1.toByte(), 2, 3L, 4.0f, 5.0, BigInteger.valueOf(6))
        val (value1, value2, value3, value4, value5, value6) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
        assertEquals(4.0f, value4)
        assertEquals(5.0, value5)
        assertEquals(BigInteger.valueOf(6), value6)
    }

    @Test
    fun `can destruct Tuple7`() {
        val tuple = Tuples.Tuple7(1.toByte(), 2, 3L, 4.0f, 5.0, BigInteger.valueOf(6), Tuples.Tuple1("seven"))
        val (value1, value2, value3, value4, value5, value6, value7) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
        assertEquals(4.0f, value4)
        assertEquals(5.0, value5)
        assertEquals(BigInteger.valueOf(6), value6)
        assertEquals(Tuples.Tuple1("seven"), value7)
    }

    @Test
    fun `can destruct Tuple8`() {
        val tuple = Tuples.Tuple8(1.toByte(), 2, 3L, 4.0f, 5.0, BigInteger.valueOf(6), Tuples.Tuple1("seven"), null as BigDecimal?)
        val (value1, value2, value3, value4, value5, value6, value7, value8) = tuple
        assertEquals(1.toByte(), value1)
        assertEquals(2, value2)
        assertEquals(3L, value3)
        assertEquals(4.0f, value4)
        assertEquals(5.0, value5)
        assertEquals(BigInteger.valueOf(6), value6)
        assertEquals(Tuples.Tuple1("seven"), value7)
        assertEquals(null, value8)
    }
}
