package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

internal class GenerateTypeWithNiceBuildersTest {

//    @Test
//    fun `just test something`() {
//        val generatedSpec = generateTypeWithNiceBuilders(
//            "a",
//            "b",
//            "c",
//            "d",
//            "e",
//            "f",
//            ClassName("o", "x"),
//            listOf(
//                Field("xx", ClassName("kotlin", "Int"), false, listOf(
//                    FieldOverload(
//                        STRING, { from, to -> CodeBlock.of("val %N = %N.toInt()", to, from) }
//                    )
//                ))
//            )
//        )
//
//        generatedSpec.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/src/main/resources"))
//
//        assertTrue(true)
//    }

    @Test
    fun `test packaging strategy`() {
        val loadedSchemaClassic = { }::class.java.getResourceAsStream("/schema-aws-classic.json")!!

        val schemaFromJsonClassic = Json.parseToJsonElement(
            loadedSchemaClassic.bufferedReader().readText()
        )

        val typesForAwsClassic = Json.decodeFromJsonElement<TypesMap>(schemaFromJsonClassic.jsonObject["types"]!!)

        val functionsForAwsClassic =
            Json.decodeFromJsonElement<FunctionsMap>(schemaFromJsonClassic.jsonObject["functions"]!!)

        val resourcesForAwsClassic =
            Json.decodeFromJsonElement<ResourcesMap>(schemaFromJsonClassic.jsonObject["resources"]!!)

        println(
            getTypeSpecs(
                resourcesForAwsClassic,
                typesForAwsClassic,
                functionsForAwsClassic
            )
        )
    }
}