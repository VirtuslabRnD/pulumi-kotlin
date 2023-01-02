# Pulumi Kotlin SDK

Experimental support for Kotlin language in [Pulumi](https://www.pulumi.com/).

**This is a proof of concept.** [We really need your feedback](#giving-feedback) before adding more features, improving
the code and integrating with the Pulumi ecosystem.

## Navigation

- [Code examples](#code-examples)

- [Getting started](#getting-started)

- [Supported providers](#supported-providers)

- [Giving feedback](#giving-feedback)

- [Development docs](#development-docs)

- How does it work? (will be added soon)

- Challenges & next steps (will be added soon)

## Code examples

> ℹ️ You can find more examples [here](https://github.com/VirtuslabRnD/pulumi-kotlin/tree/main/examples).

### Provisioning a virtual machine on Google Cloud Platform

```kotlin
import com.pulumi.googlenative.compute.v1.kotlin.instanceResource
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val instance = instanceResource("google-native-sample-project") {
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
```

### Creating a Kubernetes deployment

```kotlin
import com.pulumi.kotlin.Pulumi
import com.pulumi.kubernetes.apps.v1.kotlin.deploymentResource

fun main() {
    Pulumi.run { ctx ->
        val deployment = deploymentResource("nginx") {
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

> ⚠️ This guide is Maven-specific. Adapting these examples to your Gradle project shouldn't be hard. Vote
> on [#197](https://github.com/VirtuslabRnD/pulumi-kotlin/issues/197) in case Gradle-specific guide would be useful for
> you.

#### 1. Install Pulumi.

To install the latest Pulumi release, run the following (see
full [installation instructions](https://www.pulumi.com/docs/reference/install/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-install)
for additional installation options):

```bash
$ curl -fsSL https://get.pulumi.com/ | sh
```

#### 2. Create a Pulumi project.

> ⚠️ There are no Kotlin-specific templates for `pulumi new` yet. You need to use Java templates (e.g. `aws-java`).

To create a new Pulumi project, you need to use the `pulumi new` command:

```bash
$ mkdir pulumi-kotlin-demo && cd pulumi-kotlin-demo
$ pulumi new aws-java
```

#### 3. Add GitHub Packages Maven repository.

> ℹ️ GitHub Packages Maven registry requires
> authentication, [even for public repos](https://github.com/orgs/community/discussions/25979). We will eventually get
> rid
> of this uneccessary step by storing all the artifacts in Maven Central.

Generate [GitHub personal access token](https://github.com/settings/tokens/new) with `read:packages` scope and
include `server` in your `~/.m2/settings.xml` file:

```xml

<servers>
    <server>
        <id>github-pulumi-kotlin</id>
        <username>{{your-github-username}}</username>
        <password>{{your-github-personal-access_token}}</password>
    </server>
</servers>
```

Then, add the following repository block to `pom.xml` in the created Pulumi project (`pulumi-kotlin-demo`):

```xml

<repository>
    <id>github-pulumi-kotlin</id>
    <name>GitHub VirtuslabRnD/pulumi-kotlin Apache Maven Packages</name>
    <url>https://maven.pkg.github.com/virtuslabrnd/pulumi-kotlin</url>
</repository>
```

#### 4. Add new or replace existing dependencies.

> ℹ️ See [the section below](#supported-providers) section for the full list of supported providers along with docs.

Include provider dependencies like [Google Native]() by copying the `dependency` block
from [our GitHub Packages page](https://github.com/orgs/VirtuslabRnD/packages?repo_name=pulumi-kotlin) and pasting it to
your `pom.xml`:

 ```xml

<dependency>
    <groupId>com.virtuslab</groupId>
    <artifactId>pulumi-google-native-kotlin</artifactId>
    <version>0.27.0.0</version>
</dependency>
 ```

Only Kotlin-specific dependency is needed, so you can replace

  ```xml

<dependency>
    <groupId>com.pulumi</groupId>
    <artifactId>aws</artifactId>
    <version>5.16.2</version>
</dependency>
 ```

with

 ```xml

<dependency>
    <groupId>com.virtuslab</groupId>
    <artifactId>pulumi-aws-kotlin</artifactId>
    <version>5.16.2.0</version>
</dependency>
 ```

#### 5. Write the code.

For inspiration, see the ["Provisioning a virtual machine on Google Cloud Platform"]() example above (it's from Pulumi
Java docs for Google Native provider, rewritten to Pulumi Kotlin).

Using IntelliJ is heavily recommended as it can guide you through type-safe Kotlin DSLs.

Additionally, there are two sources of documentation:

- Pulumi official docs (example: [Google Native]())
- Pulumi Kotlin SDK API docs (KDoc) (example: [Google Native]())

#### 6. Deploy to the cloud.

Run `pulumi up`. This provisions all cloud resources declared in your code.

## Supported providers

The table below lists the providers that are currently supported by Pulumi Kotlin SDK. A full list of all Pulumi
providers can be found [here](https://www.pulumi.com/registry/).

These Kotlin libraries serve as a wrapper on top of corresponding Java libraries.

For convenience, they follow the same versioning pattern with an additional `KOTLIN-PATCH` version
number (`MAJOR.MINOR.PATCH.KOTLIN-PATCH`). For example, version `5.16.2.0` of Pulumi Kotlin AWS provider is a wrapper on
top of version `5.16.2` of Pulumi Java AWS provider.

If the additional index is incremented (e.g. to `5.16.2.1`), it means that some updates were made to our generator and
that the Kotlin code has been improved, but the underlying Java library remained the same.

<table>
  <tr>
    <th>Name</th>
    <th>Version</th>
    <th>Maven artifact (<a href="#3-add-github-packages-maven-repository">read this first</a>)</th>
    <th>GitHub Packages</th>
    <th>Pulumi official docs</th>
    <th>Kotlin API docs</th>
  </tr>
  <tr>
    <td>cloudflare</td>
    <td>4.12.1.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-cloudflare-kotlin</artifactId>
     <version>4.12.1.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=cloudflare">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/cloudflare">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/cloudflare/4.12.1.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>slack</td>
    <td>0.3.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-slack-kotlin</artifactId>
     <version>0.3.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=slack">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/slack">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/slack/0.3.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>github</td>
    <td>4.17.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-github-kotlin</artifactId>
     <version>4.17.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=github">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/github">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/github/4.17.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>random</td>
    <td>4.6.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-random-kotlin</artifactId>
     <version>4.6.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=random">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/random">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/random/4.6.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>gcp</td>
    <td>6.43.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-gcp-kotlin</artifactId>
     <version>6.43.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=gcp">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gcp">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gcp/6.43.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>google-native</td>
    <td>0.27.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-google-native-kotlin</artifactId>
     <version>0.27.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=google-native">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/google-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/google-native/0.27.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>aws</td>
    <td>5.16.2.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-aws-kotlin</artifactId>
     <version>5.16.2.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=aws">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/aws">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/aws/5.16.2.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>aws-native</td>
    <td>0.42.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-aws-native-kotlin</artifactId>
     <version>0.42.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=aws-native">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/aws-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/aws-native/0.42.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>azure</td>
    <td>5.24.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-azure-kotlin</artifactId>
     <version>5.24.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=azure">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/azure">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/azure/5.24.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>azure-native</td>
    <td>1.87.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-azure-native-kotlin</artifactId>
     <version>1.87.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=azure-native">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/azure-native">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/azure-native/1.87.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>kubernetes</td>
    <td>3.22.1.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-kubernetes-kotlin</artifactId>
     <version>3.22.1.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=kubernetes">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/kubernetes">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/kubernetes/3.22.1.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>nomad</td>
    <td>0.3.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-nomad-kotlin</artifactId>
     <version>0.3.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=nomad">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/nomad">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/nomad/0.3.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>docker</td>
    <td>3.5.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-docker-kotlin</artifactId>
     <version>3.5.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=docker">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/docker">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/docker/3.5.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>gitlab</td>
    <td>4.9.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-gitlab-kotlin</artifactId>
     <version>4.9.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=gitlab">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/gitlab">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/gitlab/4.9.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>digitalocean</td>
    <td>4.16.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-digitalocean-kotlin</artifactId>
     <version>4.16.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=digitalocean">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/digitalocean">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/digitalocean/4.16.0.0/index.html">link</a></td>
  </tr>
  <tr>
    <td>alicloud</td>
    <td>3.28.0.0</td>
    <td> 

```xml
<dependency>
     <groupId>com.virtuslab</groupId>
     <artifactId>pulumi-alicloud-kotlin</artifactId>
     <version>3.28.0.0</version>
</dependency>
```

 </td>
    <td><a href="https://github.com/orgs/VirtuslabRnD/packages?tab=packages&amp;q=alicloud">link</a></td>
    <td><a href="https://www.pulumi.com/registry/packages/alicloud">link</a></td>
    <td><a href="https://storage.googleapis.com/pulumi-kotlin-docs/alicloud/3.28.0.0/index.html">link</a></td>
  </tr>
</table>

## Feedback

## Giving feedback

Pulumi Kotlin SDK is a proof of concept, **we really need feedback before moving on**.

- Create [an issue in this repo](https://github.com/VirtuslabRnD/pulumi-kotlin/issues) or express your opinion in the
  existing ["Support for idiomatic Kotlin"](https://github.com/pulumi/pulumi-java/issues/544) issue.
- Start a thread on [#java channel](https://pulumi-community.slack.com/archives/C03DPAY96NB) (Pulumi community Slack).
- Book a meeting [through Calendly](https://calendly.com/michalfudala/kotlin-sdk-for-pulumi-feedback) and let's talk!

## Development docs

- [Code generation and local run details](docs/code-generation.md)
- [Development guidelines](docs/development-guidelines.md)
