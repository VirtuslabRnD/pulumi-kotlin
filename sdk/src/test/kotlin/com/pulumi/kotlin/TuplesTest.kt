package com.pulumi.kotlin

import com.pulumi.core.Tuples
import kotlin.test.Test
import kotlin.test.assertEquals

class TuplesTest {

    @Test
    fun `can destruct Tuple1`() {
        val tuple = Tuples.Tuple1("one")
        val (value1) = tuple
        assertEquals("one", value1)
    }

    @Test
    fun `can destruct Tuple2`() {
        val tuple = Tuples.Tuple2("one", "two")
        val (value1, value2) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
    }

    @Test
    fun `can destruct Tuple3`() {
        val tuple = Tuples.Tuple3("one", "two", "three")
        val (value1, value2, value3) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
    }

    @Test
    fun `can destruct Tuple4`() {
        val tuple = Tuples.Tuple4("one", "two", "three", 4L)
        val (value1, value2, value3, value4) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
        assertEquals(4L, value4)
    }

    @Test
    fun `can destruct Tuple5`() {
        val tuple = Tuples.Tuple5("one", "two", "three", 4L, 5)
        val (value1, value2, value3, value4, value5) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
        assertEquals(4L, value4)
        assertEquals(5, value5)
    }

    @Test
    fun `can destruct Tuple6`() {
        val tuple = Tuples.Tuple6("one", "two", "three", 4L, 5, null as Int?)
        val (value1, value2, value3, value4, value5, value6) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
        assertEquals(4L, value4)
        assertEquals(5, value5)
        assertEquals(null, value6)
    }

    @Test
    fun `can destruct Tuple7`() {
        val tuple = Tuples.Tuple7("one", "two", "three", 4L, 5, null as Int?, Tuples.Tuple1("seven"))
        val (value1, value2, value3, value4, value5, value6, value7) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
        assertEquals(4L, value4)
        assertEquals(5, value5)
        assertEquals(null, value6)
        assertEquals(Tuples.Tuple1("seven"), value7)
    }

    @Test
    fun `can destruct Tuple8`() {
        val tuple = Tuples.Tuple8("one", "two", "three", 4L, 5, null as Int?, Tuples.Tuple1("seven"), 8.0)
        val (value1, value2, value3, value4, value5, value6, value7, value8) = tuple
        assertEquals("one", value1)
        assertEquals("two", value2)
        assertEquals("three", value3)
        assertEquals(4L, value4)
        assertEquals(5, value5)
        assertEquals(null, value6)
        assertEquals(Tuples.Tuple1("seven"), value7)
        assertEquals(8.0, value8)
    }
}
