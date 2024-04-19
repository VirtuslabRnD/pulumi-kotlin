package com.pulumi.kotlin

import com.pulumi.core.Output
import com.pulumi.kotlin.options.ResourceTransformation
import com.pulumi.resources.Resource
import com.pulumi.resources.ResourceTransformation.Args
import com.pulumi.resources.ResourceTransformation.Result
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.assertAll
import kotlin.reflect.KClass

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

internal fun <T : KotlinResource, R : Resource> mockKotlinResource(
    kotlinResourceToMock: KClass<T>,
    javaResourceEquivalentToMock: KClass<R>,
): T {
    val mockedKotlinResource = mockkClass(kotlinResourceToMock)
    val mockedJavaResource = mockkClass(javaResourceEquivalentToMock)

    every { mockedKotlinResource.underlyingJavaResource } returns mockedJavaResource
    every { GlobalResourceMapper.tryMap(mockedJavaResource) } returns mockedKotlinResource

    return mockedKotlinResource
}

internal fun assertResourceTransformationResultEquals(
    expectedResult: Result,
    actualTransformations: List<ResourceTransformation>,
    transformationArgs: Args,
) {
    if (actualTransformations.isEmpty()) {
        fail<Result>("Resource transformation results are empty!")
    }

    val actualToExpectedResult = actualTransformations
        .map { it.apply(transformationArgs) }
        // assert that after invocation of transformation the invocation results would be the same as expected,
        .map { { assertTrue { it!!.args() == expectedResult.args() && it.options() == expectedResult.options() } } }

    assertAll(actualToExpectedResult)
}
