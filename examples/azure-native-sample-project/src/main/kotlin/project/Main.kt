package project

import com.pulumi.Context
import com.pulumi.azurenative.compute.kotlin.enums.CachingTypes.ReadWrite
import com.pulumi.azurenative.compute.kotlin.enums.DiskCreateOptionTypes.FromImage
import com.pulumi.azurenative.compute.kotlin.enums.StorageAccountTypes.Standard_LRS
import com.pulumi.azurenative.compute.kotlin.enums.VirtualMachineSizeTypes.Basic_A0
import com.pulumi.azurenative.compute.kotlin.virtualMachineResource
import com.pulumi.azurenative.network.kotlin.networkInterfaceResource
import com.pulumi.azurenative.network.kotlin.subnetResource
import com.pulumi.azurenative.network.kotlin.virtualNetworkResource
import com.pulumi.azurenative.resources.kotlin.resourceGroupResource
import com.pulumi.kotlin.Pulumi
import com.pulumi.random.kotlin.randomPasswordResource

fun main() {
    Pulumi.run { ctx: Context ->
        val resourceGroup = resourceGroupResource("azure-native-sample-project") {
            args {}
        }

        val mainVirtualNetwork = virtualNetworkResource("virtual-network") {
            args {
                resourceGroupName(resourceGroup.name)
                addressSpace {
                    addressPrefixes("10.0.0.0/16")
                }
            }
        }

        val internalSubnet = subnetResource("internal-subnet") {
            args {
                resourceGroupName(resourceGroup.name)
                virtualNetworkName(mainVirtualNetwork.name)
                addressPrefix("10.0.2.0/24")
            }
        }

        val mainNetworkInterface = networkInterfaceResource("network-interface") {
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

        val randomAdminPassword = randomPasswordResource("random-admin-password") {
            args {
                length(20)
                special(true)
            }
        }

        val virtualMachine = virtualMachineResource("virtual-machine") {
            args {
                resourceGroupName(resourceGroup.name)
                networkProfile {
                    networkInterfaces {
                        id(mainNetworkInterface.id)
                    }
                }
                hardwareProfile {
                    vmSize(Basic_A0)
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
