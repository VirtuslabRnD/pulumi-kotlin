package project

import com.pulumi.kotlin.Pulumi
import com.pulumi.kubernetes.apps.v1.kotlin.deploymentResource

fun main() {
    Pulumi.run { ctx ->
        val labels = mapOf("app" to "nginx")
        val deployment = deploymentResource("nginx") {
            args {
                spec {
                    selector {
                        matchLabels(labels)
                    }
                    replicas(1)
                    template {
                        metadata {
                            labels(labels)
                        }
                        spec {
                            containers(
                                {
                                    name("nginx")
                                    image("nginx")
                                    ports(
                                        {
                                            containerPort(80)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        val name = deployment.metadata
            ?.applyValue { it.name.orEmpty() }

        ctx.export("name", name)
    }
}
