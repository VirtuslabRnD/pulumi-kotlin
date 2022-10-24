package com.virtuslab.pulumikotlin.codegen.sdk

import com.pulumi.core.Output
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.ResourceMapper
import com.virtuslab.pulumikotlin.extractOutputValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.reflect.KClass
import com.pulumi.resources.CustomResource as JavaCustomResource
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource

internal class ResourceMappingTest {

    var customResourceMock = mockKotlinResource(KotlinResource::class, JavaCustomResource::class)
    var providerResourceMock =
        mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

    @BeforeEach
    fun prepareMappers() {
        /**
         * Two mappers were defined in order to simulate existence of multiple mappers to choose from in runtime
         */
        val customResourceMapperMock = mockResourceMapper(customResourceMock, customResourceMock.javaResource)
        val providerResourceMapperMock = mockResourceMapper(providerResourceMock, providerResourceMock.javaResource)

        /**
         * Mappers should be registered before use, otherwise [GlobalResourceMapper]
         * will throw [IllegalStateException], creating a resource with type-safe builder ensures registration of mapper
         */
        GlobalResourceMapper.registerMapper(customResourceMapperMock)
        GlobalResourceMapper.registerMapper(providerResourceMapperMock)
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll()

        /**
         * Each test can modify internal contents of mappers' set,
         * it should be cleared in order to avoid conflicts between tests
         */
        GlobalResourceMapper.clearMappers()
    }

    @Test
    fun `mapper corresponding to java type CustomResource should be chosen and resource should be mapped`() {
        // when
        val mappedCustomResource = GlobalResourceMapper.tryMap(customResourceMock.javaResource)

        // then
        assertEquals(customResourceMock, mappedCustomResource)
    }

    @Test
    fun `mapper corresponding to java type ProviderResource should be chosen and resource should be mapped`() {
        // when
        val mappedProviderResource = GlobalResourceMapper.tryMap(providerResourceMock.javaResource)

        // then
        assertEquals(providerResourceMock, mappedProviderResource)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `CustomResource wrapped in optional should be properly mapped`() {
        // given
        val optionalJavaCustomResource =
            Optional.of(customResourceMock.javaResource) as Optional<JavaResource?>?

        // when
        val mappedCustomResource = GlobalResourceMapper.tryMap(optionalJavaCustomResource)

        // then
        assertEquals(customResourceMock, mappedCustomResource)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `ProviderResource wrapped in optional should be properly mapped`() {
        // given
        val optionalJavaProviderResource =
            Optional.of(providerResourceMock.javaResource) as Optional<JavaResource?>?

        // when
        val mappedProviderResource = GlobalResourceMapper.tryMap(optionalJavaProviderResource)

        // then
        assertEquals(providerResourceMock, mappedProviderResource)
    }

    @Test
    fun `CustomResource wrapped in Output should be properly mapped`() {
        // given
        val outputJavaCustomResource: Output<JavaResource?>? =
            Output.of(customResourceMock.javaResource)

        // when
        val mappedCustomResource = GlobalResourceMapper.tryMap(outputJavaCustomResource)

        // then
        val mappedOutputCustomResourceContents = extractOutputValue(mappedCustomResource)

        assertEquals(customResourceMock, mappedOutputCustomResourceContents)
    }

    @Test
    fun `ProviderResource wrapped in Output should be properly mapped`() {
        // given
        val outputJavaProviderResource: Output<JavaResource?>? =
            Output.of(providerResourceMock.javaResource)

        // when
        val mappedProviderResource = GlobalResourceMapper.tryMap(outputJavaProviderResource)

        // then
        val mappedOutputProviderResourceContents = extractOutputValue(mappedProviderResource)

        assertEquals(providerResourceMock, mappedOutputProviderResourceContents)
    }

    @Test
    fun `mapping of null should return null`() {
        // given
        val resource: JavaResource? = null

        // when
        val mappedValue = GlobalResourceMapper.tryMap(resource)

        // then
        assertNull(mappedValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `mapping of empty Optional should return null`() {
        // given
        val emptyOptionalResource =
            Optional.empty<JavaResource?>() as Optional<JavaResource?>?

        // when
        val mappedValue = GlobalResourceMapper.tryMap(emptyOptionalResource)

        // then
        assertNull(mappedValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `mapping of empty Output should return empty Output`() {
        // given
        val emptyOutputResource = Output.ofNullable(null) as Output<JavaResource?>?

        // when
        val mappedValue = GlobalResourceMapper.tryMap(emptyOutputResource)

        // then
        val mappedOutputContents = extractOutputValue(mappedValue)
        assertNull(mappedOutputContents)
    }

    @Test
    fun `IllegalStateException should be thrown, when corresponding mapper was not registered in context`() {
        // given
        // BeforeEach method mocks mappers, it should be revoked before this test
        // to simulate lack of corresponding mapper
        unmockkAll()
        GlobalResourceMapper.clearMappers()

        val customResourceMock =
            mockKotlinResource(KotlinResource::class, JavaCustomResource::class)

        // when - then
        assertThrows<IllegalArgumentException> {
            GlobalResourceMapper.tryMap(customResourceMock.javaResource)
        }
    }

    private fun <T : KotlinResource, R : JavaResource> mockKotlinResource(
        kotlinResourceClass: KClass<T>,
        javaResourceEquivalentClass: KClass<R>,
    ): KotlinResource {
        val kotlinResourceMock = mockkClass(kotlinResourceClass)
        val javaResourceMock = mockkClass(javaResourceEquivalentClass)

        every {
            kotlinResourceMock.javaResource
        } returns javaResourceMock

        return kotlinResourceMock
    }

    private inline fun <reified T : KotlinResource, reified R : JavaResource> mockResourceMapper(
        kotlinResource: T,
        javaResource: R,
    ): ResourceMapper<T> {
        val resourceMapperMock = mockk<ResourceMapper<T>>()

        every {
            resourceMapperMock.doesSupportMappingOfType(ofType(JavaResource::class))
        } answers {
            /**
             * This call is default implementation of [ResourceMapper.doesSupportMappingOfType]
             * generated by [com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourceMapperGenerator]
             */
            firstArg<JavaResource>()::class == kotlinResource.javaResource::class
        }

        every {
            resourceMapperMock.map(ofType(javaResource::class))
        } returns kotlinResource

        return resourceMapperMock
    }
}
