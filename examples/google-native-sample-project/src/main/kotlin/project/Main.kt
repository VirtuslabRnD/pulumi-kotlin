package project

import com.pulumi.googlenative.compute.v1.kotlin.instance
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val instance = instance("google-native-sample-project") {
            args {
                machineType("e2-micro")
                zone("europe-central2-a")
                tags {
                    items("foo", "bar")
                }
                disks {
                    boot(true)
                    autoDelete(true)
                    initializeParams {
                        sourceImage("projects/debian-cloud/global/images/family/debian-11")
                    }
                }
                networkInterfaces {
                    network("global/networks/default")
                }
                metadata {
                    items(
                        {
                            key("foo")
                            value("bar")
                        },
                        {
                            key("startup-script")
                            value("echo hi > /test.txt")
                        },
                    )
                }
                serviceAccounts {
                    scopes("https://www.googleapis.com/auth/cloud-platform")
                }
            }
        }
        ctx.export("instanceName", instance.name)
    }
}
