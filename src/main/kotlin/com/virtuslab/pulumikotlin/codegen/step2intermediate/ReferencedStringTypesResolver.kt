package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap

class ReferencedStringTypesResolver(types: TypesMap) {

    private val rootTypesWithUnchangedTokens = types.keys

    fun shouldGenerateStringType(referencedToken: String) =
        !containsOnlyLowercaseCharacters(referencedToken) && !rootTypesWithUnchangedTokens.contains(referencedToken)

    private fun containsOnlyLowercaseCharacters(value: String) = !value.any { it.isUpperCase() }
}
