package org.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.EnumValue
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addStandardSuppressAnnotations
import org.virtuslab.pulumikotlin.codegen.step3codegen.ToKotlin.enumFunction
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDocsIfAvailable
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.enumFunction

private const val PROPERTY_NAME = "javaValue"

object EnumTypeGenerator {

    fun generateEnums(
        types: List<RootType>,
        typeNameClashResolver: TypeNameClashResolver,
    ): List<FileSpec> {
        return types.filterIsInstance<EnumType>()
            .map {
                val javaClass = typeNameClashResolver.javaNames(it.metadata).kotlinPoetClassName
                val kotlinClass = typeNameClashResolver.kotlinNames(it.metadata).kotlinPoetClassName
                buildEnumFileSpec(buildEnumTypeSpec(it, javaClass, kotlinClass), kotlinClass)
            }
    }

    private fun buildEnumTypeSpec(enumType: EnumType, javaClass: ClassName, kotlinClass: ClassName): TypeSpec {
        return TypeSpec.enumBuilder(kotlinClass)
            .addSuperinterface(convertibleToJavaClass().parameterizedBy(javaClass))
            .addFunction(enumFunction(javaClass))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(enumFunction(javaClass, kotlinClass))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder(PROPERTY_NAME, javaClass)
                    .initializer(PROPERTY_NAME)
                    .build(),
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(PROPERTY_NAME, javaClass)
                    .build(),
            )
            .addDocsIfAvailable(enumType.metadata.kDoc)
            .addEnumConstants(enumType.possibleValues, javaClass)
            .build()
    }

    private fun buildEnumFileSpec(
        enumTypeSpec: TypeSpec,
        kotlinClass: ClassName,
    ): FileSpec {
        return FileSpec.builder(kotlinClass.packageName, kotlinClass.simpleName)
            .addType(enumTypeSpec)
            .addStandardSuppressAnnotations()
            .build()
    }

    private fun TypeSpec.Builder.addEnumConstants(
        possibleValues: List<EnumValue>,
        javaEnumClassName: ClassName,
    ): TypeSpec.Builder {
        possibleValues.forEach {
            addEnumConstant(it, javaEnumClassName)
        }
        return this
    }

    private fun TypeSpec.Builder.addEnumConstant(enumValue: EnumValue, javaEnumClassName: ClassName): TypeSpec.Builder {
        val escapedEnumValue = try {
            EnumValueGenerator.makeSafeEnumName(enumValue.value)
        } catch (e: IllegalArgumentException) {
            throw InvalidEnumName(javaEnumClassName, e)
        }
        return addEnumConstant(
            escapedEnumValue,
            TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%T.%L", javaEnumClassName, escapedEnumValue)
                .addDocsIfAvailable(enumValue.kDoc)
                .build(),
        )
    }

    class InvalidEnumName(className: ClassName, cause: Throwable) :
        IllegalArgumentException(
            "Failed to generate enum name for Java class $className",
            cause,
        )
}
