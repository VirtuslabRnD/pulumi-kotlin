package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.pulumi.core.Output
import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.kotlin.options.CustomResourceOptions
import com.pulumi.kotlin.options.CustomResourceOptionsBuilder
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

object MoreTypes {

    object Kotlin {
        object Pulumi {
            fun applySuspendExtensionMethod() = MemberName("com.pulumi.kotlin", "applySuspend")

            fun pulumiDslMarkerAnnotation() = PulumiTagMarker::class.asClassName()

            fun convertibleToJavaClass() = ConvertibleToJava::class.asClassName()

            fun customResourceOptionsClass() = CustomResourceOptions::class.asClassName()

            fun customResourceOptionsBuilderClass() = CustomResourceOptionsBuilder::class.asClassName()
        }

        fun coroutinesFutureAwaitExtensionMethod() = MemberName("kotlinx.coroutines.future", "await")

        fun pairClass() = Pair::class.asTypeName()
    }

    object Java {
        object Pulumi {
            fun outputOfMethod() = outputClass().member("of")

            fun outputClass() = Output::class.asClassName()
        }
    }
}
