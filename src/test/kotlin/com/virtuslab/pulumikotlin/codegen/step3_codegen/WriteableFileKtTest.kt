package com.virtuslab.pulumikotlin.codegen.step3_codegen

import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals


class WriteableFileKtTest {

    @Test
    fun testPathDifference() {
        val shorterPath = Path("/a/b/c")
        val longerPath = Path("/a/b/c/d/e")
        val pathDifference = pathDifference(shorterPath, longerPath)
        assertEquals(Path("/d/e"), pathDifference)
    }
}