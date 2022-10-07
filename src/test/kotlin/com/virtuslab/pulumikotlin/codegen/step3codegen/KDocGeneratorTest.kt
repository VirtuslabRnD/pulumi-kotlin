package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals

private const val OUTPUT_DIRECTORY = "build/tmp/kdoc-test"
private const val PACKAGE_NAME = "com.virtuslab"

internal class KDocGeneratorTest {

    @Test
    fun `replaces spaces with hard spaces`() {
        val description = "This is a text"
        val className = "HardSpaces"

        assertSanitizedDescriptionEquals(description, "This·is·a·text")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * This is a text
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment closings`() {
        val description = "/*This description /*opens three new comments: /*"
        val className = "CommentClosing"

        assertSanitizedDescriptionEquals(description, "/*This·description·/*opens·three·new·comments:·/*\n*/*/*/")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * /*This description /*opens three new comments: /*
              | * */*/*/
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment openings`() {
        val description = "*/This description */closes three new comments: */"
        val className = "CommentOpening"

        assertSanitizedDescriptionEquals(description, "·/*·/*·/*\n*/This·description·*/closes·three·new·comments:·*/")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | *  /* /* /*
              | * */This description */closes three new comments: */
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment openings and closings`() {
        val description = "*/This /*description */closes three new comments/* and opens two: */"
        val className = "CommentClosingAndOpening"

        assertSanitizedDescriptionEquals(
            description,
            "·/*·/*·/*\n*/This·/*description·*/closes·three·new·comments/*·and·opens·two:·*/\n*/*/",
        )

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | *  /* /* /*
              | * */This /*description */closes three new comments/* and opens two: */
              | * */*/
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes trailing newlines and prevents ktlint from messing with the formatting`() {
        val description = "There are seven spaces at the end of this line       \n"
        val className = "TrailingNewlines"

        assertSanitizedDescriptionEquals(description, "There·are·seven·spaces·at·the·end·of·this·line\n")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * There are seven spaces at the end of this line
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `escapes percentage signs and prevents kotlin poet from interpreting it as a placeholder`() {
        val description = "This%would%break%code%generation"
        val className = "PercentageSigns"

        assertSanitizedDescriptionEquals(description, "This%%would%%break%%code%%generation")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * This%would%break%code%generation
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes examples tags`() {
        val description =
            """{{% examples %}}
              |There are some examples here.
              |{{% example %}}
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "ExamplesTags"

        assertSanitizedDescriptionEquals(description, "There·are·some·examples·here.\n")

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * There are some examples here.
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes non-java code snippets`() {
        val description =
            """{{% examples %}}
              |## Examples
              |{{% example %}}
              |### Specific example 1
              |```python
              |x = 2 + 2
              |```
              |```java
              |val x = 2 + 2;
              |```
              |```kotlin
              |val x = 2 + 2
              |```
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "OnlyJavaStays"

        assertSanitizedDescriptionEquals(
            description,
            """##·Examples
              |###·Specific·example·1
              |```java
              |val·x·=·2·+·2;
              |```
              |"""
                .trimMargin(),
        )

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * ## Examples
              | * ### Specific example 1
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes non-java code snippets when multiple examples are used`() {
        val codeSnippets =
            """```python
              |x = 2 + 2
              |```
              |```java
              |val x = 2 + 2;
              |```
              |```kotlin
              |val x = 2 + 2
              |```"""
                .trimMargin()
        val description =
            """{{% examples %}}
              |## Examples
              |{{% example %}}
              |### Specific example 1
              |$codeSnippets
              |{{% /example %}}    
              |{{% example %}}                
              |### Specific example 2
              |$codeSnippets
              |{{% /example %}}    
              |{{% example %}}                
              |### Specific example 3
              |$codeSnippets
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "OnlyJavaStaysWhenMultipleExamples"

        assertSanitizedDescriptionEquals(
            description,
            """##·Examples
              |###·Specific·example·1
              |```java
              |val·x·=·2·+·2;
              |```
              |###·Specific·example·2
              |```java
              |val·x·=·2·+·2;
              |```
              |###·Specific·example·3
              |```java
              |val·x·=·2·+·2;
              |```
              |"""
                .trimMargin(),
        )

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * ## Examples
              | * ### Specific example 1
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | * ### Specific example 2
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | * ### Specific example 3
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes non-java code snippets when no java is available`() {
        val description =
            """{{% examples %}}
              |## Examples
              |{{% example %}}
              |### Specific example 1
              |```python
              |x = 2 + 2
              |```
              |```kotlin
              |val x = 2 + 2
              |```
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "NoCodeSnippetsIfNoJava"

        assertSanitizedDescriptionEquals(
            description,
            """##·Examples
              |###·Specific·example·1
              |No·Java·example·available."""
                .trimMargin(),
        )

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * ## Examples
              | * ### Specific example 1
              | * No Java example available.
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes non-java code snippets when no java is available in one of multiple examples`() {
        val codeSnippets =
            """```python
              |x = 2 + 2
              |```
              |```java
              |val x = 2 + 2;
              |```
              |```kotlin
              |val x = 2 + 2
              |```"""
        val description =
            """{{% examples %}}
              |## Examples
              |{{% example %}}
              |### Specific example 1
              |$codeSnippets
              |{{% /example %}}    
              |{{% example %}}                
              |### Specific example 2
              |```python
              |x = 2 + 2
              |```
              |```kotlin
              |val x = 2 + 2
              |```
              |{{% /example %}}    
              |{{% example %}}                
              |### Specific example 3
              |$codeSnippets
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "NoCodeSnippetsIfNoJavaInExample"

        assertSanitizedDescriptionEquals(
            description,
            """##·Examples
              |###·Specific·example·1
              |```java
              |val·x·=·2·+·2;
              |```
              |###·Specific·example·2
              |No·Java·example·available.
              |###·Specific·example·3
              |```java
              |val·x·=·2·+·2;
              |```
              |"""
                .trimMargin(),
        )

        assertKDocContentsEqual(
            className,
            description,
            """/**
              | * ## Examples
              | * ### Specific example 1
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | * ### Specific example 2
              | * No Java example available.
              | * ### Specific example 3
              | * ```java
              | * val x = 2 + 2;
              | * ```
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    private fun assertSanitizedDescriptionEquals(description: String, expected: String) {
        var sanitizedDescription = ""
        KDocGenerator.addKDoc(
            { format, _ -> sanitizedDescription = format },
            description,
        )

        assertEquals(expected, sanitizedDescription)
    }

    private fun assertKDocContentsEqual(className: String, description: String, expected: String) {
        FileSpec.builder(PACKAGE_NAME, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addProperty(
                        PropertySpec.builder("value", String::class)
                            .initializer("\"hello world\"")
                            .build(),
                    )
                    .let {
                        KDocGenerator.addKDoc(
                            { format, _ -> it.addKdoc(format) },
                            description,
                        )
                        it
                    }
                    .build(),
            )
            .build()
            .writeTo(File(OUTPUT_DIRECTORY))

        assertContains(
            getFile(className).readText(),
            expected,
        )
    }

    private fun assertExportedFileCompiles(className: String) {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.fromPath(getFile(className)))
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }

    private fun getFile(className: String) =
        File("$OUTPUT_DIRECTORY/${PACKAGE_NAME.replace(".", "/")}/$className.kt")
}
