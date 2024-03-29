package project

import com.pulumi.gcp.compute.kotlin.ComputeFunctions
import com.pulumi.gcp.compute.kotlin.instance
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val debianImage = ComputeFunctions.getImage {
            family("debian-11")
            project("debian-cloud")
        }

        val instance = instance("gcp-sample-project") {
            args {
                machineType("e2-micro")
                zone("europe-central2-a")
                tags("foo", "bar")
                bootDisk {
                    autoDelete(true)
                    initializeParams {
                        image(debianImage.name)
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
        }

        ctx.export("instanceName", instance.name)
    }
}
