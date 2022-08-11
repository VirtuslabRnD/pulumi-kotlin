package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.FileSpec
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.name

typealias FileName = String
typealias Contents = String

interface WriteableFile {
    fun writeTo(destination: String)
    fun writeTo(outputStream: OutputStream): FileName

    fun get(): Pair<FileName, Contents> {
        return ByteArrayOutputStream().let {
            val fileName = writeTo(it)
            val result = fileName to it.toString(Charsets.UTF_8)
            it.close()
            result
        }
    }
}

/**
 * Example: pathDifference("/a/b/c", "/a/b/c/d/e") == "d/e"
 */
fun pathDifference(shorterPath: Path, longerPath: Path): Path {
    val shorterList = shorterPath.toList()
    val longerList = longerPath.toList()
    require(shorterList.size < longerList.size)

    val differenceSize = longerList.size - shorterList.size
    val shouldBeShorterList = longerList.dropLast(differenceSize)
    require(shouldBeShorterList == shorterList)

    return longerList.takeLast(differenceSize).reduce { path1, path2 -> path1.resolve(path2) }
}

class ExistingFile(private val basePath: String, private val path: String, private val packagePath: String): WriteableFile {
    override fun writeTo(destination: String) {
        val pathDiff = pathDifference(Path.of(basePath), Path.of(path))
        val realDestination = Path.of(destination).resolve(pathDiff)

        File(path).copyRecursively(Path.of(destination).resolve(packagePath).toFile(), overwrite = true)
    }

    override fun writeTo(outputStream: OutputStream): FileName {
        outputStream.bufferedWriter().use {
            it.write(File(path).readText())
        }
        return Path.of(path).name
    }
}

class InMemoryGeneratedFile(private val fileSpec: FileSpec): WriteableFile {
    override fun writeTo(destination: String) {
        fileSpec.writeTo(File(destination))
    }

    override fun writeTo(outputStream: OutputStream): FileName {
        OutputStreamWriter(outputStream).use {
            fileSpec.writeTo(it)
        }
        return fileSpec.name
    }

}