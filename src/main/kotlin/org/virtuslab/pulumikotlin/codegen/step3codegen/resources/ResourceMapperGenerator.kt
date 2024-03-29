package org.virtuslab.pulumikotlin.codegen.step3codegen.resources

import com.pulumi.kotlin.ResourceMapper
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject

object ResourceMapperGenerator {

    private const val MAPPER_NAME_SUFFIX = "Mapper"

    private const val FUNCTION_NAME_DOES_SUPPORT_MAPPING_OF_TYPE = "supportsMappingOfType"

    private const val FUNCTION_NAME_MAP = "map"

    private const val LITERAL_JAVA_RESOURCE = "javaResource"

    fun generateMapper(resource: ResourceType): TypeSpec {
        val javaFlags = NamingFlags(Depth.Root, Subject.Resource, Direction.Output, LanguageType.Java)
        val kotlinFlags = NamingFlags(Depth.Root, Subject.Resource, Direction.Output, LanguageType.Kotlin)

        val names = resource.name
        val kotlinResourceClassName = ClassName(names.toResourcePackage(kotlinFlags), names.toResourceName(kotlinFlags))
        val javaResourceClassName = ClassName(names.toResourcePackage(javaFlags), names.toResourceName(javaFlags))

        val mapperObjectName = names.toResourceName(kotlinFlags) + MAPPER_NAME_SUFFIX
        val parametrizedMapperInterfaceTypeName =
            ResourceMapper::class.asTypeName().parameterizedBy(kotlinResourceClassName)

        return TypeSpec.objectBuilder(mapperObjectName)
            .addSuperinterface(parametrizedMapperInterfaceTypeName)
            .addFunction(createSupportsMappingOfTypeFunction(javaResourceClassName))
            .addFunction(createMapFunction(kotlinResourceClassName, javaResourceClassName))
            .build()
    }

    private fun createSupportsMappingOfTypeFunction(javaResourceClassName: ClassName): FunSpec {
        return FunSpec.builder(FUNCTION_NAME_DOES_SUPPORT_MAPPING_OF_TYPE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(LITERAL_JAVA_RESOURCE, com.pulumi.resources.Resource::class)
            .returns(Boolean::class)
            .addStatement("return %T::class·==·%L::class", javaResourceClassName, LITERAL_JAVA_RESOURCE)
            .build()
    }

    private fun createMapFunction(kotlinResourceClassName: ClassName, javaResourceClassName: ClassName): FunSpec {
        return FunSpec.builder(FUNCTION_NAME_MAP)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(LITERAL_JAVA_RESOURCE, com.pulumi.resources.Resource::class)
            .returns(kotlinResourceClassName)
            .addStatement("return %T(%L as %T)", kotlinResourceClassName, LITERAL_JAVA_RESOURCE, javaResourceClassName)
            .build()
    }
}
