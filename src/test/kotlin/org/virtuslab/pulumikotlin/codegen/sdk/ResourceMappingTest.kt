package org.virtuslab.pulumikotlin.codegen.sdk

import com.pulumi.core.Output
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.ResourceMapper
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
import org.virtuslab.pulumikotlin.extractOutputValue
import java.util.Optional
import kotlin.reflect.KClass
import com.pulumi.resources.CustomResource as JavaCustomResource
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource

internal class ResourceMappingTest {

    var customResourceMock = mockKotlinResource(KotlinResource::class, JavaCustomResource::class)
    var providerResourceMock = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

    @BeforeEach
    fun prepareMappers() {
        /**
         * Two mappers were defined in order to simulate existence of multiple mappers to choose from in runtime
         */
        val customResourceMapperMock = mockResourceMapper(customResourceMock, customResourceMock.underlyingJavaResource)
        val providerResourceMapperMock = mockResourceMapper(
            providerResourceMock,
            providerResourceMock.underlyingJavaResource,
        )

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
        val mappedCustomResource = GlobalResourceMapper.tryMap(customResourceMock.underlyingJavaResource)

        // then
        assertEquals(customResourceMock, mappedCustomResource)
    }

    @Test
    fun `mapper corresponding to java type ProviderResource should be chosen and resource should be mapped`() {
        // when
        val mappedProviderResource = GlobalResourceMapper.tryMap(providerResourceMock.underlyingJavaResource)

        // then
        assertEquals(providerResourceMock, mappedProviderResource)
    }

    @Test
    fun `CustomResource wrapped in optional should be properly mapped`() {
        // given
        val optionalJavaCustomResource = Optional.of(customResourceMock.underlyingJavaResource)

        // when
        val mappedCustomResource = GlobalResourceMapper.tryMap(optionalJavaCustomResource)

        // then
        assertEquals(customResourceMock, mappedCustomResource)
    }

    @Test
    fun `ProviderResource wrapped in optional should be properly mapped`() {
        // given
        val optionalJavaProviderResource = Optional.of(providerResourceMock.underlyingJavaResource)

        // when
        val mappedProviderResource = GlobalResourceMapper.tryMap(optionalJavaProviderResource)

        // then
        assertEquals(providerResourceMock, mappedProviderResource)
    }

    @Test
    fun `CustomResource wrapped in Output should be properly mapped`() {
        // given
        val outputJavaCustomResource = Output.of(customResourceMock.underlyingJavaResource)

        // when
        val mappedCustomResource = GlobalResourceMapper.tryMap(outputJavaCustomResource)

        // then
        val mappedOutputCustomResourceContents = extractOutputValue(mappedCustomResource)

        assertEquals(customResourceMock, mappedOutputCustomResourceContents)
    }

    @Test
    fun `ProviderResource wrapped in Output should be properly mapped`() {
        // given
        val outputJavaProviderResource = Output.of(providerResourceMock.underlyingJavaResource)

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
    fun `mapping of empty Optional should return null`() {
        // given
        val emptyOptionalResource = Optional.empty<JavaResource>()

        // when
        val mappedValue = GlobalResourceMapper.tryMap(emptyOptionalResource)

        // then
        assertNull(mappedValue)
    }

    @Test
    fun `mapping of empty Output should return empty Output`() {
        // given
        val emptyOutputResource = Output.ofNullable<JavaResource>(null)

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
            GlobalResourceMapper.tryMap(customResourceMock.underlyingJavaResource)
        }
    }

    private fun <T : KotlinResource, R : JavaResource> mockKotlinResource(
        kotlinResourceClass: KClass<T>,
        javaResourceEquivalentClass: KClass<R>,
    ): KotlinResource {
        val kotlinResourceMock = mockkClass(kotlinResourceClass)
        val javaResourceMock = mockkClass(javaResourceEquivalentClass)

        every {
            kotlinResourceMock.underlyingJavaResource
        } returns javaResourceMock

        return kotlinResourceMock
    }

    private inline fun <reified T : KotlinResource, reified R : JavaResource> mockResourceMapper(
        kotlinResource: T,
        javaResource: R,
    ): ResourceMapper<T> {
        val resourceMapperMock = mockk<ResourceMapper<T>>()

        every {
            resourceMapperMock.supportsMappingOfType(ofType(JavaResource::class))
        } answers {
            /**
             * This call is default implementation of [ResourceMapper.supportsMappingOfType]
             * generated by [org.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourceMapperGenerator]
             */
            firstArg<JavaResource>()::class == kotlinResource.underlyingJavaResource::class
        }

        every {
            resourceMapperMock.map(ofType(javaResource::class))
        } returns kotlinResource

        return resourceMapperMock
    }
}
