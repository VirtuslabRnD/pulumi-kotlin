package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy


fun buildArgsClass(fileSpecBuilder: FileSpec.Builder, name: String, spec: Resources.Resource) {
    val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")
    val output = ClassName("com.pulumi.core", "Output")

    val customArgs = ClassName("com.pulumi.kotlin", "CustomArgs")

    val outputOf = output.member("of")



    val argsClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Args")
    val argsBuilderClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "ArgsBuilder")

    val argsClass = constructDataClass(argsClassName, spec.inputProperties, shouldWrapWithOutput = true)

    val argNames = spec.inputProperties.keys.map {
        "${it.value} = ${it.value}"
    }.joinToString(", ")

    val argsBuilderClass = TypeSpec
        .classBuilder(argsBuilderClassName)
        .addAnnotation(dslTag)
        .addProperties(
            spec.inputProperties.map {
                PropertySpec
                    .builder(it.key.value, output.parameterizedBy(referenceName(it.value)).copy(nullable = true))
                    .initializer("null")
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            }
        )
        .addFunctions(
            spec.inputProperties.flatMap {
                listOf(
                    FunSpec
                        .builder(it.key.value)
                        .addParameter("value", output.parameterizedBy(referenceName(it.value)).copy(nullable = true))
                        .addCode("this.${it.key.value} = value")
                        .build(),
                    FunSpec
                        .builder(it.key.value)
                        .addParameter("value", referenceName(it.value).copy(nullable = true))
                        .addCode("this.${it.key.value} = value?.let { %T.%M(value) }", output, outputOf)
                        .build()
                )
            }
        )
        .addFunction(
            FunSpec.builder("build")
                .returns(argsClassName)
                .addCode("return %T(${argNames})", argsClassName)
                .build()
        )
        .build()

    val resourceClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Resource")

    val resourceClass = TypeSpec
        .classBuilder(resourceClassName)
        .addAnnotation(dslTag)
        .addProperties(
            listOf(
                PropertySpec.builder("name", STRING.copy(nullable = true))
                    .mutable(true)
                    .initializer("null")
                    .build(),
                PropertySpec.builder("args", argsClassName.copy(nullable = true))
                    .mutable(true)
                    .initializer("null")
                    .build(),
                PropertySpec.builder("opts", customArgs)
                    .mutable(true)
                    .initializer("%T()", customArgs)
                    .build()
            )
        )
        .addFunction(
            FunSpec.builder("build")
                .addCode("println(name); println(args); println(opts)")
                .build()
        )
        .build()

    val argsFunction = FunSpec
        .builder("args")
        .receiver(resourceClassName)
        .addModifiers(KModifier.SUSPEND)
        .addParameter("block", LambdaTypeName.get(
            argsBuilderClassName,
            returnType = UNIT
        ))
        .addStatement("val builder = %T()", argsBuilderClassName)
        .addStatement("block(builder)")
        .addStatement("this.args = builder.build()")
        .build()

    fileSpecBuilder
        .addType(argsClass)
        .addType(argsBuilderClass)
        .addType(resourceClass)
        .addFunction(argsFunction)
}


fun generateResources(resourcesMap: ResourcesMap): GeneratedResources {
    val files = resourcesMap.entries.map { (name, spec) ->

        val file = FileSpec.builder(resourcePackageNameForName(name), fileNameForName(name).capitalize())

        buildArgsClass(file, name, spec)
        file.build()
    }

    return GeneratedResources(files, emptySet(), emptySet())
}

data class GeneratedResources(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)
