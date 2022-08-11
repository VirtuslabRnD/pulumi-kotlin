package com.virtuslab.pulumikotlin.codegen.step1schemaparse

fun Resources.SpecificationReference.withoutThePrefix(): String {
    return value.removePrefix("#/types/")
}