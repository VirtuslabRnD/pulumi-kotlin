package project

import com.pulumi.gcp.compute.kotlin.instanceResource
import com.pulumi.gcp.kotlin.Provider
import com.pulumi.gcp.kotlin.providerResource
import com.pulumi.kotlin.Pulumi

private val commonTags = listOf("gcp-provider-sample-project", "foo", "bar")

fun main() {
    Pulumi.run { ctx ->
        val providerEuropeCentral2A = createProvider(
            "provider-europe-central2-a",
            "jvm-lab",
            "europe-central2",
            "europe-central2-a",
        )

        val providerEuropeNorth1C = createProvider(
            "provider-europe-north1-c",
            "jvm-lab",
            "europe-north1",
            "europe-north1-c",
        )

        val instanceEuropeCentral2A = createInstanceWithProvider(
            "instance-europe-central2-a",
            providerEuropeCentral2A,
            commonTags + "zone-europe-central2-a",
        )

        val instanceEuropeNorth1C = createInstanceWithProvider(
            "instance-europe-north1-c",
            providerEuropeNorth1C,
            commonTags + "zone-europe-north1-c",
        )

        ctx.export("instanceEuropeCentral2AName", instanceEuropeCentral2A.name)
        ctx.export("instanceEuropeNorth1CName", instanceEuropeNorth1C.name)
        ctx.export("instanceEuropeCentral2AZone", instanceEuropeCentral2A.zone)
        ctx.export("instanceEuropeNorth1CZone", instanceEuropeNorth1C.zone)
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

private suspend fun createInstanceWithProvider(resourceName: String, provider: Provider, tags: List<String>) =
    instanceResource(resourceName) {
        args {
            machineType("e2-micro")
            tags(tags)
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
