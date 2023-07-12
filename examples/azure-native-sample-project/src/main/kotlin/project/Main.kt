package project

import com.pulumi.azurenative.compute.kotlin.enums.CachingTypes.ReadWrite
import com.pulumi.azurenative.compute.kotlin.enums.DiskCreateOptionTypes.FromImage
import com.pulumi.azurenative.compute.kotlin.enums.DiskDeleteOptionTypes
import com.pulumi.azurenative.compute.kotlin.enums.StorageAccountTypes.Standard_LRS
import com.pulumi.azurenative.compute.kotlin.enums.VirtualMachineSizeTypes.Standard_B1s
import com.pulumi.azurenative.compute.kotlin.virtualMachine
import com.pulumi.azurenative.network.kotlin.networkInterface
import com.pulumi.azurenative.network.kotlin.subnet
import com.pulumi.azurenative.network.kotlin.virtualNetwork
import com.pulumi.azurenative.resources.kotlin.resourceGroup
import com.pulumi.kotlin.Pulumi
import com.pulumi.random.kotlin.randomPassword

fun main() {
    Pulumi.run { ctx ->
        val resourceGroup = resourceGroup("azure-native-sample-project")

        val mainVirtualNetwork = virtualNetwork("virtual-network") {
            args {
                resourceGroupName(resourceGroup.name)
                addressSpace {
                    addressPrefixes("10.0.0.0/16")
                }
            }
        }

        val internalSubnet = subnet("internal-subnet") {
            args {
                resourceGroupName(resourceGroup.name)
                virtualNetworkName(mainVirtualNetwork.name)
                addressPrefix("10.0.2.0/24")
            }
        }

        val mainNetworkInterface = networkInterface("network-interface") {
            args {
                resourceGroupName(resourceGroup.name)
                ipConfigurations {
                    name("testconfiguration1")
                    subnet {
                        id(internalSubnet.id)
                    }
                    privateIPAllocationMethod("Dynamic")
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
                networkProfile {
                    networkInterfaces {
                        id(mainNetworkInterface.id)
                    }
                }
                hardwareProfile {
                    vmSize(Standard_B1s)
                }
                storageProfile {
                    imageReference {
                        publisher("Canonical")
                        offer("UbuntuServer")
                        sku("16.04-LTS")
                        version("latest")
                    }
                    osDisk {
                        name("myosdisk1")
                        caching(ReadWrite)
                        createOption(FromImage)
                        managedDisk {
                            storageAccountType(Standard_LRS)
                        }
                        deleteOption(DiskDeleteOptionTypes.Delete)
                    }
                }
                osProfile {
                    computerName("hostname")
                    adminUsername("testadmin")
                    adminPassword(randomAdminPassword.result)

                    linuxConfiguration {
                        disablePasswordAuthentication(false)
                    }
                }
                tags("foo" to "bar")
            }
        }
        ctx.export("virtualMachineId", virtualMachine.id)
    }
}
