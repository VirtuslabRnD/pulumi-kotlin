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
    fun `does not add line breaks to very long lines`() {
        val description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
            "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit " +
            "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat " +
            "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        val className = "LongLines"

        assertKDocContentEquals(
            className,
            description,
            """/**
              | * $description
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment closings`() {
        val description = "/*This description /*/*/*opens five new comments: /*"
        val className = "CommentClosing"

        assertKDocContentEquals(
            className,
            description,
            """/**
              | * /*This description /*/*/*opens five new comments: /*
              | * */*/*/*/*/
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment openings`() {
        val description = "*/This description */*/*/closes five new comments: */"
        val className = "CommentOpening"

        assertKDocContentEquals(
            className,
            description,
            """/**
              | *  /* /* /* /* /*
              | * */This description */*/*/closes five new comments: */
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds extra comment openings and closings`() {
        val description = "*/This /*description */closes three /*/*/ new comments/* and opens four: */"
        val className = "CommentClosingAndOpening"

        assertKDocContentEquals(
            className,
            description,
            """/**
              | *  /* /* /*
              | * */This /*description */closes three /*/*/ new comments/* and opens four: */
              | * */*/*/*/
              | */"""
                .trimMargin(),
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `removes trailing newlines which prevents ktlint from messing with the formatting`() {
        val description = "There are seven spaces at the end of this line       \n"
        val className = "TrailingNewlines"

        assertKDocContentEquals(
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
    fun `correctly generates docs that contain percentage signs which kotlin poet uses as placeholders`() {
        val description = "This%would%break%code%generation"
        val className = "PercentageSigns"

        assertKDocContentEquals(
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

        assertKDocContentEquals(
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

        assertKDocContentEquals(
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
    fun `generates docs that contain dollar signs`() {
        val description =
            """Hello $0
              |{{% examples %}}
              |## Examples $1
              |{{% example %}}
              |### Specific example 1 $2
              |```java
              |// this comment could break a regex $3
              |val x = 2 + 2;
              |```
              |{{% /example %}}
              |{{% /examples %}}"""
                .trimMargin()
        val className = "DollarSigns"

        assertKDocContentEquals(
            className,
            description,
            """/**
              | * Hello $0
              | * ## Examples $1
              | * ### Specific example 1 $2
              | * ```java
              | * // this comment could break a regex $3
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

        assertKDocContentEquals(
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

        assertKDocContentEquals(
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

        assertKDocContentEquals(
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

    @Test
    fun `adds deprecation warning with message`() {
        val message = "This is deprecated"
        val className = "DeprecationWarning"

        assertDeprecationMessageEquals(
            className,
            message,
            "@Deprecated(message = \"\"\"\nThis is deprecated\n\"\"\")",
        )

        assertExportedFileCompiles(className)
    }

    @Test
    fun `adds deprecation warning with multi-line message`() {
        val message = "This is deprecated.\nDon't use it.\nUse another class instead."
        val className = "MultiLineDeprecationWarning"

        assertDeprecationMessageEquals(
            className,
            message,
            "@Deprecated(message = \"\"\"\nThis is deprecated.\nDon't use it.\nUse another class instead.\n\"\"\")",
        )

        assertExportedFileCompiles(className)
    }

    private fun assertKDocContentEquals(className: String, description: String, expected: String) {
        writeTypeToFile(
            className,
            getTypeSpecBuilder(className)
                .addDocs(description)
                .build(),
        )

        assertContains(
            getFile(className).readText(),
            expected,
        )
    }

    private fun assertDeprecationMessageEquals(className: String, message: String, expected: String) {
        writeTypeToFile(
            className,
            getTypeSpecBuilder(className)
                .addDeprecationWarningIfAvailable(KDoc(description = null, deprecationMessage = message))
                .build(),
        )

        assertContains(
            getFile(className).readText(),
            expected,
        )
    }

    private fun writeTypeToFile(className: String, typeSpec: TypeSpec) {
        FileSpec.builder(PACKAGE_NAME, className)
            .addType(typeSpec)
            .build()
            .writeTo(File(OUTPUT_DIRECTORY))
    }

    private fun getTypeSpecBuilder(className: String) = TypeSpec.classBuilder(className)
        .addProperty(
            PropertySpec.builder("value", String::class)
                .initializer("\"hello world\"")
                .build(),
        )

    private fun getFile(className: String) =
        File("$OUTPUT_DIRECTORY/${PACKAGE_NAME.replace(".", "/")}/$className.kt")

    private fun assertExportedFileCompiles(className: String) {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.fromPath(getFile(className)))
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }
}
