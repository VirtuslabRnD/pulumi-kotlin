package org.virtuslab.pulumikotlin.codegen.utils

const val DEFAULT_PROVIDER_TOKEN = "pulumi:providers:Provider"

object Constants {

    val DUPLICATED_TYPES = mapOf(
        "azure-native:network:IpAllocationMethod" to "azure-native:network:IPAllocationMethod",
        "alicloud:alb/ListenerXforwardedForConfig:ListenerXforwardedForConfig" to
            "alicloud:alb/ListenerXForwardedForConfig:ListenerXForwardedForConfig",
        "azure-native:dbforpostgresql:StorageAutogrow" to "azure-native:dbforpostgresql:StorageAutoGrow",
        // note: this is wrong, but it's what is generated in Java
        // see: https://github.com/pulumi/pulumi-azure-native/issues/4107
        "azure-native:apimanagement:LlmDiagnosticSettings" to "azure-native:apimanagement:LLMDiagnosticSettings",
    )
}
