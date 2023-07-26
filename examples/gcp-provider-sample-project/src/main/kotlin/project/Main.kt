package project

import com.pulumi.core.Output
import com.pulumi.gcp.compute.kotlin.instance
import com.pulumi.gcp.kotlin.GcpProvider
import com.pulumi.gcp.kotlin.gcpProvider
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

        val instanceEuropeCentral2AOutputMap = mapOf(
            "instanceName" to instanceEuropeCentral2A.name,
            "instanceZone" to instanceEuropeCentral2A.zone,
        )
        val instanceEuropeNorth1COutputMap = mapOf(
            "instanceName" to instanceEuropeNorth1C.name,
            "instanceZone" to instanceEuropeNorth1C.zone,
        )

        ctx.export("instanceEuropeCentral2A", Output.of(instanceEuropeCentral2AOutputMap))
        ctx.export("instanceEuropeNorth1C", Output.of(instanceEuropeNorth1COutputMap))
    }
}

private suspend fun createProvider(resourceName: String, projectName: String, region: String, zone: String) =
    gcpProvider(resourceName) {
        args {
            project(projectName)
            region(region)
            zone(zone)
        }
    }

private suspend fun createInstanceWithProvider(resourceName: String, provider: GcpProvider, tags: List<String>) =
    instance(resourceName) {
        args {
            machineType("e2-micro")
            tags(tags)
            bootDisk {
                autoDelete(true)
                initializeParams {
                    image("debian-cloud/debian-11")
                }
            }
            networkInterfaces {
                network("default")
            }
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
