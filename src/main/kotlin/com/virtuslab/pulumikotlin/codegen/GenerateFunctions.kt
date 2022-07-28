package com.virtuslab.pulumikotlin.codegen

import com.pulumi.core.TypeShape
import com.pulumi.deployment.Deployment
import com.pulumi.deployment.DeploymentInstance
import com.pulumi.kotlin.PulumiJavaKotlinInterop
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

inline fun <reified T> classNameOf(): ClassName {
    return T::class.asClassName()
}

inline fun <reified T: Any, R> memberOf(f: KFunction<R>): MemberName {
    return classNameOf<T>().member(f.name)
}

fun <T: Any, R> classWithMember(c: KClass<T>, f: KFunction<R>): MemberName {
    return c.asClassName().member(f.name)
}

fun ClassName.member(f: KFunction<Any>): MemberName {
    return member(f.name)
}

//fun ClassName.member3(f: ): MemberName {
//    return member(f.toString())
//}

object FunctionTypeLocations {

}

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



    val mappedArgsBlock = CodeBlock.of(
        "val mappedArgs = %M(args) as %T",
        classNameOf<PulumiJavaKotlinInterop>().member(PulumiJavaKotlinInterop::toJava),
        invokeArgs
    )

    val javaResulTypeBlock = CodeBlock.of(
        "val javaResultType = %M(%N::class.java)",
        classNameOf<PulumiJavaKotlinInterop>().member(PulumiJavaKotlinInterop::getTargetClassForFromKotlinToJava),
        outputType
    )

    val typeShapeBlock = CodeBlock.of(
        "val typeShape = %M(javaResultType)",
        classNameOf<TypeShape<*>>().member("of")
    )

    val invokeOptionsBlock = CodeBlock.of("val invokeOptions = %M(%M)", utilitiesWithVersion, invokeOptionsEmpty)

    val resultBlock = CodeBlock.of("val result = %M().%N(%S, typeShape, args, invokeOptions)",
        classNameOf<Deployment>().member(Deployment::getInstance),
        classNameOf<DeploymentInstance>().member("invoke"),
        name
    )

    val awaitedResult = CodeBlock.of("val awaitedResult = result.%M()", awaitFuture)

    val returnValue = CodeBlock.of("return %T.%M(awaitedResult)", pulumiInterop, toKotlin)

    val codeBuilder = CodeBlock.builder()
    listOf(
        mappedArgsBlock,
        javaResulTypeBlock,
        typeShapeBlock,
        invokeOptionsBlock,
        resultBlock,
        awaitedResult,
        returnValue
    ).forEach { block -> codeBuilder.add(block) }

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

                val i = constructDataClass(inputName, function.inputs)
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