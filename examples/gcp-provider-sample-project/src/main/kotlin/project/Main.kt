package project

import com.pulumi.gcp.compute.kotlin.instanceResource
import com.pulumi.gcp.kotlin.Provider
import com.pulumi.gcp.kotlin.providerResource
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val providerEuropeCentral2A = createProvider(
            "gcp-provider-sample-project-provider-europe-central2-a",
            "jvm-lab",
            "europe-central2",
            "europe-central2-a",
        )

        val providerEuropeNorth1C = createProvider(
            "gcp-provider-sample-project-provider-europe-north1-c",
            "jvm-lab",
            "europe-north1",
            "europe-north1-c",
        )

        val instanceEuropeCentral2A = createInstanceWithProvider(
            "gcp-provider-sample-project-instance-europe-central2-a",
            providerEuropeCentral2A,
        )

        val instanceEuropeNorth1C = createInstanceWithProvider(
            "gcp-provider-sample-project-instance-europe-north1-c",
            providerEuropeNorth1C,
        )

        ctx.export("gcp-provider-sample-project-instance-europe-central2-a-name", instanceEuropeCentral2A.name)
        ctx.export("gcp-provider-sample-project-instance-europe-north1-c-name", instanceEuropeNorth1C.name)
        ctx.export("gcp-provider-sample-project-instance-europe-central2-a-zone", instanceEuropeCentral2A.zone)
        ctx.export("gcp-provider-sample-project-instance-europe-north1-c-zone", instanceEuropeNorth1C.zone)
    }
}

private suspend fun createProvider(resourceName: String, projectName: String, region: String, zone: String) =
    providerResource(resourceName) {
        args {
            project(projectName)
            region(region)
            zone(zone)
        }
    }

private suspend fun createInstanceWithProvider(resourceName: String, provider: Provider) =
    instanceResource(resourceName) {
        args {
            machineType("e2-micro")
            tags("foo", "bar")
            bootDisk {
                initializeParams {
                    image("debian-cloud/debian-11")
                }
            }
            networkInterfaces(
                {
                    network("default")
                },
            )
            metadata("foo" to "bar")
            metadataStartupScript("echo hi > /test.txt")
            serviceAccount {
                scopes("cloud-platform")
            }
        }
        opts {
            provider(provider)
        }
    }
