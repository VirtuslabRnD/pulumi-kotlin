package com.virtuslab.pulumikotlin.codegen.step1schemaparse

val SchemaModel.ReferenceProperty.referencedTypeName: String
    get() = ref.referencedTypeName

val SchemaModel.SpecificationReference.referencedTypeName: String
    get() = value.removePrefix("#/types/")
