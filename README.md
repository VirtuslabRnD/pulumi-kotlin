</br>
<p align="center">
   <img width="40" src="https://github.com/VirtuslabRnD/pulumi-kotlin/assets/4415632/01eaed9a-2acc-455c-a2e7-0c945406447c" />
</p>

# Pulumi Kotlin SDK

<p align="center">
    <img src="https://github.com/VirtuslabRnD/pulumi-kotlin/assets/4415632/feb4d51e-f223-4a80-a164-ed6cb4500526" />
</p>

Experimental support for Kotlin language in [Pulumi](https://www.pulumi.com/).

**This is a proof of concept.** [We really need your feedback](#giving-feedback) before adding more features, improving
the code and integrating with the Pulumi ecosystem.

## Navigation

- [Articles and talks](#articles-and-talks)

- [Code examples](#code-examples)

- [Getting started](#getting-started)

- [Supported providers](#supported-providers)

- [Giving feedback](#giving-feedback)

- [Development docs](#development-docs)

## Articles and talks

- [Pulumi Kotlin. The missing piece in Kotlin multi-platform](https://medium.com/virtuslab/pulumi-kotlin-9c0cb2d53e1c) by [@jplewa](https://github.com/jplewa)
- [Infrastructure as code done right with JVM](https://2023.javazone.no/program/444fd903-4857-4591-b0c2-a824ccf5d3af) by [@lbialy](https://github.com/lbialy)

## Code examples

> ℹ️ You can find more examples [here][pulumi-kotlin-code-examples].

### Provisioning a virtual machine on Google Cloud Platform

```kotlin
import com.pulumi.gcp.compute.kotlin.ComputeFunctions
import com.pulumi.gcp.compute.kotlin.instance
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val debianImage = ComputeFunctions.getImage {
            family("debian-11")
            project("debian-cloud")
        }
    
        val instance = instance("gcp-sample-instance") {
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
```

### Provisioning a virtual machine on Azure
```kotlin
import com.pulumi.azurenative.compute.kotlin.enums.CachingTypes.ReadWrite
import com.pulumi.azurenative.compute.kotlin.enums.DiskCreateOptionTypes.FromImage
import com.pulumi.azurenative.compute.kotlin.enums.DiskDeleteOptionTypes.Delete
import com.pulumi.azurenative.network.kotlin.enums.IPAllocationMethod.Dynamic
import com.pulumi.azurenative.compute.kotlin.enums.StorageAccountTypes.Standard_LRS
import com.pulumi.azurenative.compute.kotlin.enums.VirtualMachineSizeTypes.Standard_B1s
import com.pulumi.azurenative.compute.kotlin.virtualMachine
import com.pulumi.azurenative.network.kotlin.networkInterface
import com.pulumi.azurenative.network.kotlin.subnet
import com.pulumi.azurenative.network.kotlin.virtualNetwork
import com.pulumi.azurenative.resources.kotlin.resourceGroup
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val resourceGroup = resourceGroup("azure-native-sample-vm")

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
                    privateIPAllocationMethod(Dynamic)
                }
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
                        deleteOption(Delete)
                    }
                }
                osProfile {
                    computerName("hostname")
                    adminUsername("testadmin")
                    adminPassword("testpassword")
                }
            }
        }

        ctx.export("virtualMachineId", virtualMachine.id)
    }
}
```

### Creating a Kubernetes deployment

```kotlin
import com.pulumi.kotlin.Pulumi
import com.pulumi.kubernetes.apps.v1.kotlin.deployment

fun main() {
    Pulumi.run { ctx ->
        val deployment = deployment("nginx") {
            args {
                spec {
                    selector {
                        matchLabels("app" to "nginx")
                    }
                    replicas(1)
                    template {
                        metadata {
                            labels("app" to "nginx")
                        }
                        spec {
                            containers {
                                name("nginx")
                                image("nginx")
                                ports {
                                    containerPort(80)
                                }
                            }
                        }
                    }
                }
            }
        }

        val name = deployment.metadata?.applyValue { it.name.orEmpty() }

        ctx.export("name", name)
    }
}
```

## Getting started

#### 1. Install Pulumi.

To install the latest Pulumi release, run the following 
(see full [installation instructions][pulumi-installation-guide] for additional installation options):

```bash
$ curl -fsSL https://get.pulumi.com/ | sh
```

#### 2. Create a Pulumi project.

> ⚠️ There are no Kotlin-specific templates for `pulumi new` yet. You need to use Java templates (e.g. `aws-java`).

To create a new Pulumi project, you need to use the `pulumi new` command.

##### Maven

```bash
$ mkdir pulumi-kotlin-demo && cd pulumi-kotlin-demo
$ pulumi new aws-java
```

##### Gradle

```bash
$ mkdir pulumi-kotlin-demo && cd pulumi-kotlin-demo
$ pulumi new https://github.com/myhau/pulumi-templates/tree/add-template-for-java-gradle-with-kotlin-dsl/java-gradle-kts
```

#### 3. Add new or replace existing dependencies

> ℹ️ See [the section below](#supported-providers) for the full list of supported providers along with docs.

##### Maven

Copy the `dependency` block from [the table below (Supported providers)](#supported-providers) and paste it to your 
`pom.xml`. For example:

```xml
<dependency>
    <groupId>org.virtuslab</groupId>
    <artifactId>pulumi-google-native-kotlin</artifactId>
    <version>0.31.0.0</version>
</dependency>
```

Any existing Java-specific dependency can be replaced with its Kotlin-specific equivalent. For example:

```xml
<dependency>
    <groupId>com.pulumi</groupId>
    <artifactId>aws</artifactId>
    <version>5.42.0</version>
</dependency>
```

can be replaced with:

```xml

<dependency>
    <groupId>org.virtuslab</groupId>
    <artifactId>pulumi-aws-kotlin</artifactId>
    <version>5.42.0.0</version>
</dependency>
```

Similarly, when it comes to the base Pulumi dependency, you can replace this:

```xml
<dependency>
    <groupId>com.pulumi</groupId>
    <artifactId>pulumi</artifactId>
    <version>0.9.4</version>
</dependency>
```

with this:

```xml
<dependency>
    <groupId>org.virtuslab</groupId>
    <artifactId>pulumi-kotlin</artifactId>
    <version>0.9.4.0</version>
</dependency>
```

##### Gradle

Copy the dependency from [the table below (Supported providers)](#supported-providers) and paste it to your
`build.gradle.kts`. For example:

```kt
implementation("org.virtuslab:pulumi-google-native-kotlin:0.31.0.0")
```

Any existing Java-specific dependency can be replaced with its Kotlin-specific equivalent. For example:

```kt
implementation("com.pulumi.aws:5.42.0")
```

can be replaced with: 

```kt
implementation("org.virtuslab.pulumi-aws-kotlin:5.42.0.0")
```

Similarly, when it comes to the base Pulumi dependency, you can replace this:

```kt
implementation("com.pulumi:pulumi:0.9.4")
```

with this:

```kt
implementation("org.virtuslab:pulumi-kotlin:0.9.4.0")
```

#### 4. Read the docs.

> ⚠️ For now, docs viewable in your text editor (and Kotlin API docs hosted [here][google-native-kdoc]) 
> only include Java SDK example snippets. 

Using IntelliJ is heavily recommended as it can guide you through type-safe Kotlin DSLs.

Additionally, there are two sources of documentation:

- Pulumi official docs (example for [`google-native`][google-native-registry-docs])
- Kotlin API docs (example for [`google-native`][google-native-kdoc])

#### 5. Write the code.

Write the code that describes your resources: 

```kotlin
import com.pulumi.kotlin.Pulumi

fun main() {
  Pulumi.run { ctx ->
    // your resources
  }
}
```

For inspiration, see the
["Provisioning a virtual machine on Google Cloud Platform"](#provisioning-a-virtual-machine-on-google-cloud-platform)
example above (it's from Pulumi Java docs for Google Native provider, rewritten to Pulumi Kotlin).

#### 6. Deploy to the cloud.

Run `pulumi up`. This provisions all cloud resources declared in your code.

## Supported providers

The table below lists the providers that are currently supported by Pulumi Kotlin SDK. A full list of all Pulumi
providers can be found [here][pulumi-registry].

These Kotlin libraries serve as a wrapper on top of corresponding Java libraries.

For convenience, they follow the same versioning pattern with an additional `KOTLIN-PATCH` version
number (`MAJOR.MINOR.PATCH.KOTLIN-PATCH`). For example, version `5.16.2.0` of Pulumi Kotlin AWS provider is a wrapper on
top of version `5.16.2` of Pulumi Java AWS provider.

If the additional index is incremented (e.g. to `5.16.2.1`), it means that some updates were made to our generator and
that the Kotlin code has been improved, but the underlying Java library remained the same.

[//]: # (This table is generated automatically during releases, don't alter it manually unless absolutely necessary.)
<table id="pulumi-kotlin-versions-table">
  <tr>
    <th>Name</th>
    <th>Version</th>
    <th>Maven artifact</th>
    <th>Gradle artifact</th>
    <th>Maven Central</th>
    <th>Pulumi official docs</th>
    <th>Kotlin API docs</th>
  </tr>
  <tr>
    <td>cloudflare</td>
    <td>4.16.0.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-cloudflare-kotlin</artifactId>
     <version>4.16.0.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-cloudflare-kotlin:4.16.0.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-cloudflare-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/cloudflare">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/cloudflare/4.16.0.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>cloudflare</td>
    <td>5.22.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-cloudflare-kotlin</artifactId>
     <version>5.22.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-cloudflare-kotlin:5.22.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-cloudflare-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/cloudflare">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/cloudflare/5.22.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>slack</td>
    <td>0.4.4.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-slack-kotlin</artifactId>
     <version>0.4.4.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-slack-kotlin:0.4.4.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-slack-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/slack">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/slack/0.4.4.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>github</td>
    <td>5.26.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-github-kotlin</artifactId>
     <version>5.26.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-github-kotlin:5.26.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-github-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/github">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/github/5.26.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>github</td>
    <td>6.1.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-github-kotlin</artifactId>
     <version>6.1.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-github-kotlin:6.1.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-github-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/github">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/github/6.1.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>random</td>
    <td>4.16.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-random-kotlin</artifactId>
     <version>4.16.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-random-kotlin:4.16.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-random-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/random">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/random/4.16.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>gcp</td>
    <td>6.67.1.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-gcp-kotlin</artifactId>
     <version>6.67.1.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-gcp-kotlin:6.67.1.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-gcp-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gcp">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gcp/6.67.1.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>gcp</td>
    <td>7.11.2.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-gcp-kotlin</artifactId>
     <version>7.11.2.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-gcp-kotlin:7.11.2.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-gcp-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gcp">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gcp/7.11.2.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>google-native</td>
    <td>0.31.1.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-google-native-kotlin</artifactId>
     <version>0.31.1.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-google-native-kotlin:0.31.1.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-google-native-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/google-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/google-native/0.31.1.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>aws</td>
    <td>5.43.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-aws-kotlin</artifactId>
     <version>5.43.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-aws-kotlin:5.43.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-aws-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/aws">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/aws/5.43.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>aws</td>
    <td>6.27.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-aws-kotlin</artifactId>
     <version>6.27.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-aws-kotlin:6.27.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-aws-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/aws">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/aws/6.27.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>aws-native</td>
    <td>0.99.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-aws-native-kotlin</artifactId>
     <version>0.99.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-aws-native-kotlin:0.99.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-aws-native-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/aws-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/aws-native/0.99.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>azure</td>
    <td>5.68.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-azure-kotlin</artifactId>
     <version>5.68.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-azure-kotlin:5.68.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-azure-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/azure">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/azure/5.68.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>azure-native</td>
    <td>1.104.0.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-azure-native-kotlin</artifactId>
     <version>1.104.0.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-azure-native-kotlin:1.104.0.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-azure-native-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/azure-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/azure-native/1.104.0.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>azure-native</td>
    <td>2.33.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-azure-native-kotlin</artifactId>
     <version>2.33.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-azure-native-kotlin:2.33.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-azure-native-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/azure-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/azure-native/2.33.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>kubernetes</td>
    <td>3.30.2.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-kubernetes-kotlin</artifactId>
     <version>3.30.2.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-kubernetes-kotlin:3.30.2.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-kubernetes-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/kubernetes">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/kubernetes/3.30.2.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>kubernetes</td>
    <td>4.9.1.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-kubernetes-kotlin</artifactId>
     <version>4.9.1.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-kubernetes-kotlin:4.9.1.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-kubernetes-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/kubernetes">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/kubernetes/4.9.1.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>nomad</td>
    <td>0.4.1.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-nomad-kotlin</artifactId>
     <version>0.4.1.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-nomad-kotlin:0.4.1.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-nomad-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/nomad">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/nomad/0.4.1.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>nomad</td>
    <td>2.2.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-nomad-kotlin</artifactId>
     <version>2.2.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-nomad-kotlin:2.2.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-nomad-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/nomad">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/nomad/2.2.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>docker</td>
    <td>3.6.1.2</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-docker-kotlin</artifactId>
     <version>3.6.1.2</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-docker-kotlin:3.6.1.2")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-docker-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/docker">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/docker/3.6.1.2/index.html">link</a></td>
  </tr>
  <tr>
    <td>docker</td>
    <td>4.5.2.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-docker-kotlin</artifactId>
     <version>4.5.2.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-docker-kotlin:4.5.2.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-docker-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/docker">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/docker/4.5.2.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>gitlab</td>
    <td>4.10.0.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-gitlab-kotlin</artifactId>
     <version>4.10.0.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-gitlab-kotlin:4.10.0.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-gitlab-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gitlab">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gitlab/4.10.0.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>gitlab</td>
    <td>5.0.2.1</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-gitlab-kotlin</artifactId>
     <version>5.0.2.1</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-gitlab-kotlin:5.0.2.1")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-gitlab-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gitlab">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gitlab/5.0.2.1/index.html">link</a></td>
  </tr>
  <tr>
    <td>gitlab</td>
    <td>6.9.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-gitlab-kotlin</artifactId>
     <version>6.9.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-gitlab-kotlin:6.9.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-gitlab-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gitlab">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gitlab/6.9.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>digitalocean</td>
    <td>4.27.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-digitalocean-kotlin</artifactId>
     <version>4.27.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-digitalocean-kotlin:4.27.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-digitalocean-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/digitalocean">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/digitalocean/4.27.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>alicloud</td>
    <td>3.50.0.0</td>
    <td> 
 
```xml
<dependency>
     <groupId>org.virtuslab</groupId>
     <artifactId>pulumi-alicloud-kotlin</artifactId>
     <version>3.50.0.0</version>
</dependency>
```
 
 </td>
    <td> 
 
```kt
implementation("org.virtuslab:pulumi-alicloud-kotlin:3.50.0.0")
```
 
 </td>
    <td><a href="https://search.maven.org/artifact/org.virtuslab/pulumi-alicloud-kotlin">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/alicloud">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/alicloud/3.50.0.0/index.html">link</a></td>
  </tr>
</table>

## Giving feedback

Pulumi Kotlin SDK is a proof of concept, **we really need feedback before moving on**.

- Create [an issue in this repo][issues-pulumi-kotlin] or express your opinion in the
  existing ["Support for idiomatic Kotlin"][support-for-idiomatic-kotlin-issue] issue.
- Start a thread on [#java channel][pulumi-slack-java-channel] (Pulumi community Slack).
- Book a meeting [through Calendly][calendly-feedback-meeting] and let's talk!

## Development docs

- [Code generation and local run details](docs/code-generation.md)
- [Development guidelines](docs/development-guidelines.md)


[pulumi-kotlin-code-examples]: https://github.com/VirtuslabRnD/pulumi-kotlin/tree/main/examples
[pulumi-kotlin-issue-197]: https://github.com/VirtuslabRnD/pulumi-kotlin/issues/197
[pulumi-installation-guide]: https://www.pulumi.com/docs/reference/install/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-install
[google-native-kdoc]: https://storage.googleapis.com/pulumi-kotlin-docs/google-native/0.31.1.0/index.html
[google-native-registry-docs]: https://www.pulumi.com/registry/packages/google-native
[pulumi-registry]: https://www.pulumi.com/registry
[issues-pulumi-kotlin]: https://github.com/VirtuslabRnD/pulumi-kotlin/issues
[support-for-idiomatic-kotlin-issue]: https://github.com/pulumi/pulumi-java/issues/544
[pulumi-slack-java-channel]: https://pulumi-community.slack.com/archives/C03DPAY96NB
[calendly-feedback-meeting]: https://calendly.com/michalfudala/kotlin-sdk-for-pulumi-feedback


<p align="center">
   <a href="https://virtuslab.com/open-source/"><img width="40" src="https://github.com/VirtuslabRnD/pulumi-kotlin/assets/4415632/01eaed9a-2acc-455c-a2e7-0c945406447c" /></a>
</p>
