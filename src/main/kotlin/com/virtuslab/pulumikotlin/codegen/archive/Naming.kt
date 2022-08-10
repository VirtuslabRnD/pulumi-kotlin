package com.virtuslab.pulumikotlin.codegen.archive

import com.squareup.kotlinpoet.ClassName

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

fun javaClassNameForNameSuffix(name: String, suffix: String): ClassName {
    return ClassName(javaPackageNameForName(name), fileNameForName(name) + suffix)
}
