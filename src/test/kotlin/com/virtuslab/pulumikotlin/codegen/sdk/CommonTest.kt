package com.virtuslab.pulumikotlin.codegen.sdk

import com.pulumi.kotlin.toJava
import com.pulumi.kotlin.toKotlin
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class CommonTest {

    @Test
    fun `converts GSON JsonElement to Kotlin JsonElement`() {
        val gsonJsonElement = com.google.gson.JsonObject()

        val nestedField = com.google.gson.JsonObject()
        nestedField.addProperty("nestedField1", "value3")
        nestedField.addProperty("nestedField2", 4)

        gsonJsonElement.addProperty("field1", "value1")
        gsonJsonElement.addProperty("field2", 2)
        gsonJsonElement.add("field3", nestedField)

        val expectedKotlinJsonElement = kotlinx.serialization.json.JsonObject(
            mapOf(
                "field1" to JsonPrimitive("value1"),
                "field2" to JsonPrimitive(2),
                "field3" to kotlinx.serialization.json.JsonObject(
                    mapOf(
                        "nestedField1" to JsonPrimitive("value3"),
                        "nestedField2" to JsonPrimitive(4),

                    ),
                ),
            ),
        )
        val kotlinJsonElement = gsonJsonElement.toKotlin()

        assertEquals(expectedKotlinJsonElement, kotlinJsonElement)
    }

    @Test
    fun `converts GSON JsonElement with null field to Kotlin JsonElement`() {
        val gsonJsonElement = com.google.gson.JsonObject()

        gsonJsonElement.addProperty("field", null as String?)

        val expectedKotlinJsonElement = kotlinx.serialization.json.JsonObject(
            mapOf(
                "field" to JsonPrimitive(null as String?),
            ),
        )
        val kotlinJsonElement = gsonJsonElement.toKotlin()

        assertEquals(expectedKotlinJsonElement, kotlinJsonElement)
    }

    @Test
    fun `converts Kotlin JsonElement to GSON JsonElement`() {
        val kotlinJsonElement = kotlinx.serialization.json.JsonObject(
            mapOf(
                "field1" to JsonPrimitive("value1"),
                "field2" to JsonPrimitive(2),
                "field3" to kotlinx.serialization.json.JsonObject(
                    mapOf(
                        "nestedField1" to JsonPrimitive("value3"),
                        "nestedField2" to JsonPrimitive(4),

                    ),
                ),
            ),
        )
        val expectedGsonJsonElement = com.google.gson.JsonObject()

        val nestedField = com.google.gson.JsonObject()
        nestedField.addProperty("nestedField1", "value3")
        nestedField.addProperty("nestedField2", 4)

        expectedGsonJsonElement.addProperty("field1", "value1")
        expectedGsonJsonElement.addProperty("field2", 2)
        expectedGsonJsonElement.add("field3", nestedField)

        val gsonJsonElement = kotlinJsonElement.toJava()

        assertEquals(expectedGsonJsonElement, gsonJsonElement)
    }

    @Test
    fun `converts Kotlin JsonElement with null field to GSON JsonElement`() {
        val kotlinJsonElement = kotlinx.serialization.json.JsonObject(
            mapOf(
                "field" to JsonPrimitive(null as String?),
            ),
        )
        val expectedGsonJsonElement = com.google.gson.JsonObject()

        expectedGsonJsonElement.addProperty("field", null as String?)

        val gsonJsonElement = kotlinJsonElement.toJava()

        assertEquals(expectedGsonJsonElement, gsonJsonElement)
    }
}
