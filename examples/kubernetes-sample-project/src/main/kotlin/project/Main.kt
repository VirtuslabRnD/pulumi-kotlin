package project

import com.pulumi.kotlin.Pulumi
import com.pulumi.kubernetes.apps.v1.kotlin.deployment

fun main() {
    Pulumi.run { ctx ->
        val deployment = deployment("nginx") {
            args {
                spec {
                    selector {
                        matchLabels("app" to "nginx")
                    }
                    replicas(1)
                    template {
                        metadata {
                            labels("app" to "nginx")
                        }
                        spec {
                            containers {
                                name("nginx")
                                image("nginx")
                                ports {
                                    containerPort(80)
                                }
                            }
                        }
                    }
                }
            }
        }

        val name = deployment.metadata?.applyValue { it.name.orEmpty() }

        ctx.export("name", name)
    }
}
