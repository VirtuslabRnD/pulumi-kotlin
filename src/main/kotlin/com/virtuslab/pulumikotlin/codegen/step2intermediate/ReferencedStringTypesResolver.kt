package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap

// TODO this class introduces a bug present in golang implementation
//  it should be removed when the bug in golang is fixed and types are properly generated
//  despite the case insensitive type tokens
//  @see https://github.com/VirtuslabRnD/pulumi-kotlin/pull/123#intentionally-generating-fields-with-incorrect-type-string
class ReferencedStringTypesResolver(types: TypesMap) {

    private val rootTypesWithUnchangedTokens = types.keys

    fun shouldGenerateStringType(referencedToken: String) = !rootTypesWithUnchangedTokens.contains(referencedToken)
}
