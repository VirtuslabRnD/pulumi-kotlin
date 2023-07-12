package project

import com.pulumi.azure.compute.kotlin.virtualMachine
import com.pulumi.azure.core.kotlin.resourceGroup
import com.pulumi.azure.network.kotlin.networkInterface
import com.pulumi.azure.network.kotlin.subnet
import com.pulumi.azure.network.kotlin.virtualNetwork
import com.pulumi.kotlin.Pulumi
import com.pulumi.random.kotlin.randomPassword

fun main() {
    Pulumi.run { ctx ->
        val resourceGroup = resourceGroup("azure-sample-project")

        val mainVirtualNetwork = virtualNetwork("virtual-network") {
            args {
                resourceGroupName(resourceGroup.name)
                addressSpaces("10.0.0.0/16")
            }
        }

        val internalSubnet = subnet("internal-subnet") {
            args {
                resourceGroupName(resourceGroup.name)
                virtualNetworkName(mainVirtualNetwork.name)
                addressPrefixes("10.0.2.0/24")
            }
        }

        val mainNetworkInterface = networkInterface("network-interface") {
            args {
                resourceGroupName(resourceGroup.name)
                ipConfigurations {
                    name("testconfiguration1")
                    subnetId(internalSubnet.id)
                    privateIpAddressAllocation("Dynamic")
                }
            }
        }

        val randomAdminPassword = randomPassword("random-admin-password") {
            args {
                length(20)
                special(true)
            }
        }

        val virtualMachine = virtualMachine("virtual-machine") {
            args {
                resourceGroupName(resourceGroup.name)
                networkInterfaceIds(mainNetworkInterface.id)
                vmSize("Standard_B1s")
                storageImageReference {
                    publisher("Canonical")
                    offer("UbuntuServer")
                    sku("16.04-LTS")
                    version("latest")
                }
                storageOsDisk {
                    name("myosdisk1")
                    caching("ReadWrite")
                    createOption("FromImage")
                    managedDiskType("Standard_LRS")
                }
                osProfile {
                    computerName("hostname")
                    adminUsername("testadmin")
                    adminPassword(randomAdminPassword.result)
                }
                osProfileLinuxConfig {
                    disablePasswordAuthentication(false)
                }
                tags("foo" to "bar")
                deleteOsDiskOnTermination(true)
            }
        }
        ctx.export("virtualMachineId", virtualMachine.id)
    }
}
