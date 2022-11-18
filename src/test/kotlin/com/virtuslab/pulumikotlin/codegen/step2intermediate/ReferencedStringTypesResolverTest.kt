package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

// FIXME this test class verifies incorrect behaviour introduced intentionally to keep consistency with Pulumi-java,
//  it should be removed, when the issue in Pulumi-java is solved
//  @see https://github.com/VirtuslabRnD/pulumi-kotlin/pull/123#intentionally-generating-fields-with-incorrect-type-string
internal class ReferencedStringTypesResolverTest {

    enum class ValidTokenCase(
        val typeToken: String,
        val typesMap: TypesMap,
        val shouldGenerateStringType: Boolean,
    ) {
        CorrectMixedCaseToken(
            "provider:module/submodule:TypeName",
            mapOf("provider:module/submodule:TypeName" to ObjectProperty()),
            false,
        ),
        IncorrectMixedCaseToken(
            "provider:module/submodule:TypeName",
            mapOf("provider:module/Submodule:TypeName" to ObjectProperty()),
            true,
        ),
        LowerCaseToken(
            "provider:module/submodule:TypeName",
            mapOf("provider:module/submodule:typename" to ObjectProperty()),
            true,
        ),
        LowerCaseInputToken(
            "provider:module/submodule:typename",
            mapOf("provider:module/submodule:typename" to ObjectProperty()),
            false,
        ),
    }

    @ParameterizedTest
    @EnumSource(ValidTokenCase::class)
    fun `should be correctly determined if string type should be generated`(testCase: ValidTokenCase) {
        // given
        val referencedStringTypesResolver = ReferencedStringTypesResolver(testCase.typesMap)

        // when
        val actualShouldGenerateStrinType = referencedStringTypesResolver.shouldGenerateStringType(testCase.typeToken)

        // then
        assertEquals(testCase.shouldGenerateStringType, actualShouldGenerateStrinType)
    }
}
