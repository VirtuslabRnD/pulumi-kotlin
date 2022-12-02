package com.virtuslab.pulumikotlin.codegen.utils

const val DEFAULT_PROVIDER_TOKEN = "pulumi:providers:Provider"

object Constants {

    val DUPLICATED_TYPES = mapOf(
        "azure-native:network:IpAllocationMethod" to "azure-native:network:IPAllocationMethod",
    )
}
