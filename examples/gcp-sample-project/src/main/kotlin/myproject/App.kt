package myproject

import com.pulumi.Context
import com.pulumi.Pulumi
import com.pulumi.gcp.compute.kotlin.instanceResource
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    Pulumi.run { _: Context ->
        runBlocking {
            instanceResource("gcp-sample-project") {
                args {
                    machineType("e2-micro")
                    zone("europe-central2-a")
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
                    metadata(mapOf("foo" to "bar"))
                    metadataStartupScript("echo hi > /test.txt")
                    serviceAccount {
                        scopes("cloud-platform")
                    }
                }
            }
        }
    }
}
