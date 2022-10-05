package com.virtuslab.pulumikotlin.codegen.step3codegen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class KDocGeneratorTest {

    @Test
    fun `replaces spaces with hard spaces`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "This is a text",
        )
        assertEquals("This·is·a·text", kDoc)
    }

    @Test
    fun `adds extra comment closings`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "/*This description /*opens three new comments: /*",
        )
        assertEquals("/*This·description·/*opens·three·new·comments:·/*\n*/*/*/", kDoc)
    }

    @Test
    fun `adds extra comment openings`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "*/This description */closes three new comments: */",
        )
        assertEquals("·/*·/*·/*\n*/This·description·*/closes·three·new·comments:·*/", kDoc)
    }

    @Test
    fun `adds extra comment openings and closings`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "*/This /*description */closes three new comments/* and opens two: */",
        )
        assertEquals("·/*·/*·/*\n*/This·/*description·*/closes·three·new·comments/*·and·opens·two:·*/\n*/*/", kDoc)
    }

    @Test
    fun `removes trailing newlines and prevents ktlint from messing with the formatting`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "There are seven spaces at the end of this line       \n",
        )
        assertEquals("There·are·seven·spaces·at·the·end·of·this·line\n", kDoc)
    }

    @Test
    fun `escapes percentage signs and prevents kotlin poet from interpreting it as a placeholder`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            "This%would%break%code%generation",
        )
        assertEquals("This%%would%%break%%code%%generation", kDoc)
    }

    @Test
    fun `removes examples tags`() {
        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            """{{% examples %}}
                |There are some examples here.
                |{{% example %}}
                |{{% /example %}}
                |{{% /examples %}}"""
                .trimMargin(),
        )
        assertEquals("There·are·some·examples·here.\n", kDoc)
    }

    @Test
    fun `removes non-java code snippets`() {
        val description = """{{% examples %}}
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
        val trimmedKDoc = """##·Examples
                |###·Specific·example·1
                |```java
                |val·x·=·2·+·2;
                |```
                |"""
            .trimMargin()

        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            description,
        )
        assertEquals(trimmedKDoc, kDoc)
    }

    @Test
    fun `removes non-java code snippets when multiple examples are used`() {
        val codeSnippets = """```python
                |x = 2 + 2
                |```
                |```java
                |val x = 2 + 2;
                |```
                |```kotlin
                |val x = 2 + 2
                |```"""
        val description = """{{% examples %}}
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
        val trimmedKDoc = """##·Examples
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
            .trimMargin()

        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            description,
        )

        assertEquals(trimmedKDoc, kDoc)
    }

    @Test
    fun `removes non-java code snippets when no java is available`() {
        val description = """{{% examples %}}
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

        val trimmedKDoc = """##·Examples
                |###·Specific·example·1
                |No·Java·example·available."""
            .trimMargin()

        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            description,
        )
        assertEquals(trimmedKDoc, kDoc)
    }

    @Test
    fun `removes non-java code snippets when no java is available in one pf multiple examples`() {
        val codeSnippets = """```python
                |x = 2 + 2
                |```
                |```java
                |val x = 2 + 2;
                |```
                |```kotlin
                |val x = 2 + 2
                |```"""
        val description = """{{% examples %}}
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

        val trimmedKDoc = """##·Examples
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
            .trimMargin()

        var kDoc = ""
        KDocGenerator.addKDoc(
            { format, _ -> kDoc = format },
            description,
        )

        assertEquals(trimmedKDoc, kDoc)
    }
}
