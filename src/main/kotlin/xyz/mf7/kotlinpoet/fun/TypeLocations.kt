package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.ClassName


object TypeLocations {
    fun inputTypeJavaClassName(pulumiReference: String): ClassName {
        val segments = pulumiReference.split("/").first().replace("-", "").split(":")
        val packageSuffix = segments.dropLast(1) + "inputs" + segments.take(1)
        val className = pulumiReference.split("/").last().replace("-", "").split(":").last().replace("-", "").capitalize()

        return ClassName("com.pulumi.${packageSuffix.joinToString(".")}", className)
    }

    fun outputTypeJavaClassName(pulumiReference: String, classSuffix: String = ""): ClassName {
        val packageSegments = pulumiReference.split("/").first().replace("-", "").split(":")
        val packageSuffix = packageSegments.dropLast(1) + "outputs" + packageSegments.take(1)
        val className = pulumiReference.split("/").last().replace("-", "").split(":").last().replace("-", "").capitalize()

        return ClassName("com.pulumi.${packageSuffix.joinToString(".")}", className + classSuffix)
    }

    fun inputKotlinClassName(pulumiReference: String): ClassName {
        val packageSegments = pulumiReference.split("/").first().replace("-", "").split(":")
        val packageSuffix = packageSegments.dropLast(1) + "input" + packageSegments.take(1)
        val className = pulumiReference.split("/").last().replace("-", "").split(":").last().replace("-", "").capitalize()

        return ClassName("com.pulumi.kotlin.${packageSuffix.joinToString(".")}", className)
    }

    fun outputKotlinClassName(pulumiReference: String, classSuffix: String = ""): ClassName {
        val packageSegments = pulumiReference.split("/").first().replace("-", "").split(":")
        val packageSuffix = packageSegments.dropLast(1) + "outputs" + packageSegments.take(1)
        val className = pulumiReference.split("/").last().replace("-", "").split(":").last().replace("-", "").capitalize()

        return ClassName("com.pulumi.kotlin.${packageSuffix.joinToString(".")}", className + classSuffix)
    }

    fun packageNameForName(name: String): String {
        return "com.pulumi.kotlin." + name.split("/").first().replace(":", ".").replace("-", "")
    }

    fun resourcePackageNameForName(name: String): String {
        return "com.pulumi.kotlin.resources." + name.split("/").first().replace(":", ".").replace("-", "")
    }

    fun javaPackageNameForName(name: String): String {
        return "com.pulumi." + name.split("/").first().replace(":", ".").replace("-", "")
    }

    fun fileNameForName(name: String): String {
        return name.split("/").last().split(":").last().replace("-", "").capitalize()
    }

    fun classNameForName(name: String): ClassName {
        return ClassName(packageNameForName(name), fileNameForName(name))
    }

    fun classNameForNameSuffix(name: String, suffix: String): ClassName {
        return ClassName(packageNameForName(name), fileNameForName(name) + suffix)
    }

}