package project

import com.pulumi.azure.compute.kotlin.virtualMachineResource
import com.pulumi.azure.core.kotlin.resourceGroupResource
import com.pulumi.azure.network.kotlin.networkInterfaceResource
import com.pulumi.azure.network.kotlin.subnetResource
import com.pulumi.azure.network.kotlin.virtualNetworkResource
import com.pulumi.kotlin.Pulumi
import com.pulumi.random.kotlin.randomPasswordResource

fun main() {
    Pulumi.run { ctx ->
        val resourceGroup = resourceGroupResource("azure-sample-project") {
            args {}
        }

        val mainVirtualNetwork = virtualNetworkResource("virtual-network") {
            args {
                resourceGroupName(resourceGroup.name)
                addressSpaces("10.0.0.0/16")
            }
        }

        val internalSubnet = subnetResource("internal-subnet") {
            args {
                resourceGroupName(resourceGroup.name)
                virtualNetworkName(mainVirtualNetwork.name)
                addressPrefixes("10.0.2.0/24")
            }
        }

        val mainNetworkInterface = networkInterfaceResource("network-interface") {
            args {
                resourceGroupName(resourceGroup.name)
                ipConfigurations {
                    name("testconfiguration1")
                    subnetId(internalSubnet.id)
                    privateIpAddressAllocation("Dynamic")
                }
            }
        }

        val randomAdminPassword = randomPasswordResource("random-admin-password") {
            args {
                length(20)
                special(true)
            }
        }

        val virtualMachine = virtualMachineResource("virtual-machine") {
            args {
                resourceGroupName(resourceGroup.name)
                networkInterfaceIds(mainNetworkInterface.id)
                vmSize("Basic_A0")
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
            }
        }
        ctx.export("virtualMachineId", virtualMachine.id)
    }
}
