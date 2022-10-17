package com.virtuslab.pulumikotlin.codegen.sdk

import com.pulumi.core.Output
import com.pulumi.kotlin.GeneralResourceMapper
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

internal class ResourceMappingTest {

    @AfterEach
    fun cleanUp() {
        unmockkAll()

        // each test can modify internal contents of mappers' set,
        // it should be cleared in order to avoid conflicts between tests
        val property = GeneralResourceMapper::class.declaredMemberProperties.find { it.name == "mappers" }
        property!!.isAccessible = true
        (property.getter.call() as MutableSet<*>).clear()
        property.isAccessible = false
    }

    @Test
    fun `mapper corresponding to type should be chosen and resource should be mapped`() {
        // given
        val customResourceMock =
            mockKotlinResource(KotlinResource::class, com.pulumi.resources.CustomResource::class)
        val providerResourceMock =
            mockKotlinResource(KotlinProviderResource::class, com.pulumi.resources.ProviderResource::class)
        val customResourceMapperMock = mockResourceMapper(customResourceMock, customResourceMock.javaResource)
        val providerResourceMapperMock = mockResourceMapper(providerResourceMock, providerResourceMock.javaResource)

        /**
         * Mappers should be registered before use, otherwise [GeneralResourceMapper]
         * will throw [IllegalStateException], creating a resource with type-safe builder ensures registration of mapper
         */
        GeneralResourceMapper.registerMapper(customResourceMapperMock)
        GeneralResourceMapper.registerMapper(providerResourceMapperMock)

        // when
        val mappedCustomResource = GeneralResourceMapper.tryMap(customResourceMock.javaResource)
        val mappedProviderResource = GeneralResourceMapper.tryMap(providerResourceMock.javaResource)

        // then
        assertAll(
            { assertEquals(customResourceMock, mappedCustomResource) },
            { assertEquals(providerResourceMock, mappedProviderResource) },
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `resources wrapped in optionals should be properly mapped`() {
        // given
        val customResourceMock =
            mockKotlinResource(KotlinResource::class, com.pulumi.resources.CustomResource::class)
        val providerResourceMock =
            mockKotlinResource(KotlinProviderResource::class, com.pulumi.resources.ProviderResource::class)
        val customResourceMapperMock = mockResourceMapper(customResourceMock, customResourceMock.javaResource)
        val providerResourceMapperMock = mockResourceMapper(providerResourceMock, providerResourceMock.javaResource)

        val optionalJavaCustomResource =
            Optional.of(customResourceMock.javaResource) as Optional<com.pulumi.resources.Resource?>?
        val optionalJavaProviderResource =
            Optional.of(providerResourceMock.javaResource) as Optional<com.pulumi.resources.Resource?>?

        /**
         * Mappers should be registered before use, otherwise [GeneralResourceMapper]
         * will throw [IllegalStateException], creating a resource with type-safe builder ensures registration of mapper
         */
        GeneralResourceMapper.registerMapper(customResourceMapperMock)
        GeneralResourceMapper.registerMapper(providerResourceMapperMock)

        // when
        val mappedCustomResource = GeneralResourceMapper.tryMap(optionalJavaCustomResource)
        val mappedProviderResource = GeneralResourceMapper.tryMap(optionalJavaProviderResource)

        // then
        assertAll(
            { assertEquals(customResourceMock, mappedCustomResource) },
            { assertEquals(providerResourceMock, mappedProviderResource) },
        )
    }

    @Test
    fun `resources wrapped in outputs should be properly mapped`() {
        // given
        val customResourceMock =
            mockKotlinResource(KotlinResource::class, com.pulumi.resources.CustomResource::class)
        val providerResourceMock =
            mockKotlinResource(KotlinProviderResource::class, com.pulumi.resources.ProviderResource::class)
        val customResourceMapperMock = mockResourceMapper(customResourceMock, customResourceMock.javaResource)
        val providerResourceMapperMock = mockResourceMapper(providerResourceMock, providerResourceMock.javaResource)

        val outputJavaCustomResource: Output<com.pulumi.resources.Resource?>? =
            Output.of(customResourceMock.javaResource)
        val outputJavaProviderResource: Output<com.pulumi.resources.Resource?>? =
            Output.of(providerResourceMock.javaResource)

        /**
         * Mappers should be registered before use, otherwise [GeneralResourceMapper]
         * will throw [IllegalStateException], creating a resource with type-safe builder ensures registration of mapper
         */
        GeneralResourceMapper.registerMapper(customResourceMapperMock)
        GeneralResourceMapper.registerMapper(providerResourceMapperMock)

        // when
        val mappedCustomResource = GeneralResourceMapper.tryMap(outputJavaCustomResource)
        val mappedProviderResource = GeneralResourceMapper.tryMap(outputJavaProviderResource)

        // then
        val mappedOutputCustomResourceContents = extractOutputValue(mappedCustomResource)
        val mappedOutputProviderResourceContents = extractOutputValue(mappedProviderResource)

        assertAll(
            { assertEquals(customResourceMock, mappedOutputCustomResourceContents) },
            { assertEquals(providerResourceMock, mappedOutputProviderResourceContents) },
        )
    }

    @Test
    fun `mapping of null should return null`() {
        // given
        val resource: com.pulumi.resources.Resource? = null

        // when
        val mappedValue = GeneralResourceMapper.tryMap(resource)

        // then
        assertNull(mappedValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `mapping of empty Optional should return null`() {
        // given
        val emptyOptionalResource =
            Optional.empty<com.pulumi.resources.Resource?>() as Optional<com.pulumi.resources.Resource?>?

        // when
        val mappedValue = GeneralResourceMapper.tryMap(emptyOptionalResource)

        // then
        assertNull(mappedValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST") // casting to avoid overload resolution ambiguity
    fun `mapping of empty Output should return empty Output`() {
        // given
        val emptyOutputResource = Output.ofNullable(null) as Output<com.pulumi.resources.Resource?>?

        // when
        val mappedValue = GeneralResourceMapper.tryMap(emptyOutputResource)

        // then
        val mappedOutputContents = extractOutputValue(mappedValue)
        assertNull(mappedOutputContents)
    }

    @Test
    fun `IllegalStateException should be thrown, when corresponding mapper was not registered in context`() {
        // given
        val customResourceMock =
            mockKotlinResource(KotlinResource::class, com.pulumi.resources.CustomResource::class)

        // when - then
        assertThrows<IllegalStateException> {
            GeneralResourceMapper.tryMap(customResourceMock.javaResource)
        }
    }

    private fun <T : KotlinResource, R : com.pulumi.resources.Resource> mockKotlinResource(
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

    private fun <T : KotlinResource, R : com.pulumi.resources.Resource> mockResourceMapper(
        kotlinResource: T,
        javaResource: R,
    ): ResourceMapper<T> {
        val resourceMapperMock = mockk<ResourceMapper<T>>()

        every {
            resourceMapperMock.doesSupportMappingOfType(any())
        } answers {
            /**
             * This call is default implementation of [ResourceMapper.doesSupportMappingOfType]
             * generated by [com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourceMapperGenerator]
             */
            firstArg<com.pulumi.resources.Resource>()::class == kotlinResource.javaResource::class
        }

        every {
            resourceMapperMock.map(javaResource)
        } returns kotlinResource

        return resourceMapperMock
    }
}
