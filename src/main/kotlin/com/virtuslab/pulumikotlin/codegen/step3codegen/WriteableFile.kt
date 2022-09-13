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

class ExistingFile(private val absoluteCopyFrom: String, private val relativeCopyTo: String) : WriteableFile {
    override fun writeTo(destination: String) {
        val resolvedDestination = Path.of(destination).resolve(relativeCopyTo)

        File(absoluteCopyFrom).copyRecursively(resolvedDestination.toFile(), overwrite = true)
    }

    override fun writeTo(outputStream: OutputStream): FileName {
        outputStream.bufferedWriter().use {
            it.write(File(absoluteCopyFrom).readText())
        }
        return Path.of(absoluteCopyFrom).name
    }
}

class InMemoryGeneratedFile(private val fileSpec: FileSpec) : WriteableFile {
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
