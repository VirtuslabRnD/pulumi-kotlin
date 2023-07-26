package org.virtuslab.pulumikotlin.codegen.step3codegen.resources

import com.pulumi.kotlin.KotlinComponentResource
import com.pulumi.kotlin.KotlinCustomResource
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.customResourceOptionsBuilderClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.customResourceOptionsClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.pulumiDslMarkerAnnotation
import org.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addStandardSuppressAnnotations
import org.virtuslab.pulumikotlin.codegen.step3codegen.ToKotlin
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDocsIfAvailable
import org.virtuslab.pulumikotlin.codegen.utils.letIf
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

object ResourceGenerator {

    private const val JAVA_RESOURCE_PARAMETER_NAME = "javaResource"

    private val fieldNamesInBaseResourceClass = KotlinResource::class.declaredMemberProperties.map { it.name }
    private val fieldNamesInBaseCustomResourceClass =
        KotlinCustomResource::class.declaredMemberProperties.map { it.name }

    fun generateResources(resources: List<ResourceType>, typeNameClashResolver: TypeNameClashResolver): List<FileSpec> {
        val files = resources.map { type ->
            val file = FileSpec.builder(
                type.name.toResourcePackage(NamingFlags(Root, Resource, Output, Kotlin)),
                type.name.toResourceName(NamingFlags(Root, Resource, Output, Kotlin)),
            )
                .addStandardSuppressAnnotations()

            buildArgsClass(file, type, typeNameClashResolver)

            file.build()
        }

        return files
    }

    private fun buildArgsClass(
        fileSpecBuilder: FileSpec.Builder,
        resourceType: ResourceType,
        typeNameClashResolver: TypeNameClashResolver,
    ) {
        val javaFlags = NamingFlags(Root, Resource, Output, Java)
        val kotlinFlags = NamingFlags(Root, Resource, Output, Kotlin)

        val names = resourceType.name
        val resourceClassName = ClassName(names.toResourcePackage(kotlinFlags), names.toResourceName(kotlinFlags))
        val javaResourceClassName = ClassName(names.toResourcePackage(javaFlags), names.toResourceName(javaFlags))

        val fields = resourceType.outputFields
            .filter { field -> !fieldNamesInBaseResourceClass.contains(field.toKotlinName()) }
            .letIf(resourceType.isComponent) {
                it.filter { field -> !fieldNamesInBaseCustomResourceClass.contains(field.toKotlinName()) }
            }
            .map { field ->
                PropertySpec
                    .builder(field.toKotlinName(), field.toTypeName(typeNameClashResolver))
                    .getter(
                        ToKotlin.resourceFunction(
                            field,
                            typeNameClashResolver,
                        ),
                    )
                    .addDocsIfAvailable(field.kDoc)
                    .addDeprecationWarningIfAvailable(field.kDoc)
                    .build()
            }

        val resourceMapperTypeSpec = ResourceMapperGenerator.generateMapper(resourceType)
        val resourceClass = TypeSpec.classBuilder(resourceClassName)
            .superclass(determineSuperclass(resourceType))
            .addProperties(
                listOf(
                    PropertySpec.builder(JAVA_RESOURCE_PARAMETER_NAME, javaResourceClassName)
                        .initializer(JAVA_RESOURCE_PARAMETER_NAME)
                        .addModifiers(OVERRIDE, INTERNAL)
                        .build(),
                ),
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(INTERNAL)
                    .addParameter(JAVA_RESOURCE_PARAMETER_NAME, javaResourceClassName)
                    .build(),
            )
            .addSuperclassConstructorParameter("%L, %N", JAVA_RESOURCE_PARAMETER_NAME, resourceMapperTypeSpec)
            .addProperties(fields)
            .addDocsIfAvailable(resourceType.kDoc)
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        val resourceBuilderClassName = ClassName(
            names.toResourcePackage(kotlinFlags),
            names.toResourceName(kotlinFlags) + "ResourceBuilder",
        )

        val argsClassName = typeNameClashResolver.toTypeName(resourceType.argsType, Kotlin)
        val argsBuilderClassName = resourceType.argsType.toBuilderTypeName()

        val argsFunction = FunSpec
            .builder("args")
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    argsBuilderClassName,
                    returnType = UNIT,
                ).copy(suspending = true),
            )
            .addStatement("val builder = %T()", argsBuilderClassName)
            .addStatement("block(builder)")
            .addStatement("this.args = builder.build()")
            .addDocs("@param block The arguments to use to populate this resource's properties.")
            .build()

        val optsFunction = FunSpec
            .builder("opts")
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    customResourceOptionsBuilderClass(),
                    returnType = UNIT,
                ).copy(suspending = true),
            )
            .addStatement("val builder = %T()", customResourceOptionsBuilderClass())
            .addStatement("block(builder)")
            .addStatement("this.opts = builder.build()")
            .addDocs("@param block A bag of options that control this resource's behavior.")
            .build()

        val resourceBuilderClass = TypeSpec
            .classBuilder(resourceBuilderClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(pulumiDslMarkerAnnotation())
            .addProperties(
                listOf(
                    PropertySpec.builder("name", STRING.copy(nullable = true))
                        .mutable(true)
                        .initializer("null")
                        .build(),
                    PropertySpec.builder("args", argsClassName)
                        .mutable(true)
                        .initializer("%T()", argsClassName)
                        .build(),
                    PropertySpec.builder("opts", customResourceOptionsClass())
                        .mutable(true)
                        .initializer("%T()", customResourceOptionsClass())
                        .build(),
                ),
            )
            .addFunction(
                FunSpec.builder("name")
                    .addParameter("value", STRING)
                    .addCode("this.name = value")
                    .addDocs("@param name The _unique_ name of the resulting resource.")
                    .build(),
            )
            .addFunction(argsFunction)
            .addFunction(optsFunction)
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(INTERNAL)
                    .let {
                        it.addCode(
                            """
                        val builtJavaResource = %T(
                            this.name,
                            this.args.toJava(),
                            this.opts.toJava()
                        )
                        """,
                            javaResourceClassName,
                        )

                        it.addCode("return %T(builtJavaResource)", resourceClassName)
                    }
                    .returns(resourceClassName)
                    .build(),
            )
            .addDocs("Builder for [${resourceClassName.simpleName}].")
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        val resourceFunction = FunSpec
            .builder(names.toResourceFunctionName(kotlinFlags))
            .addModifiers(KModifier.SUSPEND)
            .returns(resourceClassName)
            .addParameter("name", STRING)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    resourceBuilderClassName,
                    returnType = UNIT,
                ).copy(suspending = true),
            )
            .addStatement("val builder = %T()", resourceBuilderClassName)
            .addStatement("builder.name(name)")
            .addStatement("block(builder)")
            .addStatement("return builder.build()")
            .addDocs(
                """@see [${resourceType.name.name}].
                  |@param name The _unique_ name of the resulting resource.
                  |@param block Builder for [${resourceClassName.simpleName}]."""
                    .trimMargin(),
            )
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        val resourceFunctionWithoutLambda = FunSpec
            .builder(names.toResourceFunctionName(kotlinFlags))
            .returns(resourceClassName)
            .addParameter("name", STRING)
            .addStatement("val builder = %T()", resourceBuilderClassName)
            .addStatement("builder.name(name)")
            .addStatement("return builder.build()")
            .addDocs(
                """@see [${resourceType.name.name}].
                  |@param name The _unique_ name of the resulting resource."""
                    .trimMargin(),
            )
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        fileSpecBuilder
            .addType(resourceBuilderClass)
            .addType(resourceClass)
            .addType(resourceMapperTypeSpec)
            .addFunction(resourceFunction)
            .addFunction(resourceFunctionWithoutLambda)
    }

    private fun determineSuperclass(resourceType: ResourceType): KClass<*> {
        return if (resourceType.isProvider) {
            KotlinProviderResource::class
        } else if (resourceType.isComponent) {
            KotlinComponentResource::class
        } else {
            KotlinCustomResource::class
        }
    }
}
