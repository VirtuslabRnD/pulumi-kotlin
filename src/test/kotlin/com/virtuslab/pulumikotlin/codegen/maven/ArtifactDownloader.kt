package com.virtuslab.pulumikotlin.codegen.maven

import com.squareup.tools.maven.resolution.ArtifactResolver
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

object ArtifactDownloader {
    private val resolver = ArtifactResolver(cacheDir = Path("/Users/mfudala/workspace/pulumi-kotlin/artifact-cache"))

    fun download(coordinate: String): Path {
        val result = resolver.download(coordinate, downloadSources = false)

        return result.main.absolute()
    }
}