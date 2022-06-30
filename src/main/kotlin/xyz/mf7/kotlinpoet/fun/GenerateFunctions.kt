package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member


fun generateMethodBody(it: FunSpec.Builder, name: String, outputType: TypeSpec): FunSpec.Builder {
    val deployPackage = "com.pulumi.deployment"
    val corePackage = "com.pulumi.core"
    val providerPackage = "com.pulumi.kotlin.aws" // TODO: parametrize
    val deployment = ClassName(deployPackage, "Deployment")
    val deploymentInstance = ClassName(deployPackage, "DeploymentInstance")
    val getInstance = deployment.member("getInstance")
    val invokeAsync = deploymentInstance.member("invokeAsync")
    val typeShape = ClassName(corePackage, "TypeShape")
    val ofTypeShape = typeShape.member("of")
    val utilities = ClassName(providerPackage, "Utilities")
    val utilitiesWithVersion = utilities.member("withVersion")
    val invokeOptions = ClassName(deployPackage, "InvokeOptions")
    val invokeOptionsEmpty = invokeOptions.member("Empty")
    val invokeArgs = ClassName("com.pulumi.resources", "InvokeArgs")

    val pulumiInterop = ClassName("com.pulumi.kotlin", "PulumiJavaKotlinInterop")
    val getTargetClassForFromKotlinToJava = pulumiInterop.member("getTargetClassForFromKotlinToJava")
    val toJava = pulumiInterop.member("toJava")
    val toKotlin = pulumiInterop.member("toKotlin")

    val awaitFuture = MemberName("kotlinx.coroutines.future", "await")

//    val convertFrom = CodeBlock.builder().add()



    it.addStatement(
        "val mappedArgs = %T.%M(args) as %T",
        pulumiInterop,
        toJava,
        invokeArgs
    )

    it.addStatement(
        "val result = %T.%M().%N(%S, %T.%M(%T.%M(%N::class.java)), args, %T.%M(%T.%M))",
        deployment,
        getInstance,
        "invokeAsync",
        name,
        typeShape,
        ofTypeShape,
        pulumiInterop,
        getTargetClassForFromKotlinToJava,
        outputType,
        utilities,
        utilitiesWithVersion,
        invokeOptions,
        invokeOptionsEmpty
    )

    it.addStatement("val awaitedResult = result.%M()", awaitFuture)

    it.addStatement("return %T.%M(awaitedResult)",
        pulumiInterop, toKotlin
    )

    return it
}

fun generateFunctions(functionsMap: FunctionsMap): GeneratedFunction {
    val files = functionsMap
        .entries
        .groupBy { (name, value) -> name.split("/").first() }
        .flatMap { (groupName, entries) ->

            val resultTypes = mutableListOf<FileSpec>()

            val file = FileSpec.builder(packageNameForName(groupName), fileNameForName(groupName).capitalize())

            entries.forEach { (name, function) ->

                val inputName = ClassName(packageNameForName(groupName), fileNameForName(name) + "Args")
                val outputName = ClassName(packageNameForName(groupName), fileNameForName(name) + "Result")

                val i = constructDataClass(inputName, function.inputs,
                    {
                        superclass(ClassName("com.pulumi.resources", "InvokeArgs"))
                    },
                    { name, _, isRequired ->
                        addAnnotation(
                            AnnotationSpec.builder(ClassName("com.pulumi.core.annotations", "Import"))
                                .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                                .addMember("name = %S", name.value)
                                .addMember("required = %L", isRequired)
                                .build()
                        )
                    }
                )
                val inputFile = FileSpec.builder(packageNameForName(groupName), fileNameForName(name) + "Args")
                    .addType(i)
                    .build()

                resultTypes.add(inputFile)

                val o = constructDataClass(outputName, function.outputs, shouldAddCustomTypeAnnotations = true)


                val outputFile = FileSpec.builder(packageNameForName(groupName), fileNameForName(name) + "Result")
                    .addType(o)
                    .build()

                val realName = name.split(Regex("[/:]")).last()

                val funSpec = FunSpec.builder(realName)
                    .addModifiers(KModifier.SUSPEND)
                    .let { f ->
                        function.description?.let {
                            f.addKdoc("Some kdoc was here but it does not work currently")
                        }
                        f
                    }
                    .let {
                        generateMethodBody(it, name, o)
                    }
                    .addParameter("args", inputName)
                    .returns(outputName)
                    .build()

                file.addFunction(funSpec)

                resultTypes.add(outputFile)
            }

            resultTypes + listOf(file.build())
        }

    return GeneratedFunction(files, emptySet(), emptySet())
}

data class GeneratedFunction(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)