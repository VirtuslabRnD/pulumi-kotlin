# `pulumi-kotlin`

Experimental support for Kotlin language in Pulumi.

This repository contains the code used to generate Kotlin wrappers on top of the existing Pulumi Java libraries.

## What is possible with Kotlin SDK?

More examples can be found [here](https://github.com/VirtuslabRnD/pulumi-kotlin/tree/main/examples).

### VM creation on GCP

```kotlin
import com.pulumi.Context
import com.pulumi.gcp.compute.kotlin.ComputeFunctions
import com.pulumi.gcp.compute.kotlin.instanceResource
import com.pulumi.kotlin.Pulumi

fun main() {
    Pulumi.run { ctx: Context ->
        val debianImage = ComputeFunctions.getImage {
            family("debian-11")
            project("debian-cloud")
        }

        instanceResource("gcp-sample-project") {
            args {
                machineType("e2-micro")
                zone("europe-central2-a")
                tags("foo", "bar")
                bootDisk {
                    autoDelete(true)
                    initializeParams {
                        image("debian-cloud/debian-11")
                    }
                }
                networkInterfaces(
                    {
                        network("default")
                    },
                )
                metadata("foo" to "bar")
                metadataStartupScript("echo hi > /test.txt")
                serviceAccount {
                    scopes("cloud-platform")
                }
            }
        }
    }
}
```

### Kubernetes deployment

```kotlin
import com.pulumi.kotlin.Pulumi
import com.pulumi.kubernetes.apps.v1.kotlin.deploymentResource

fun main() {
   Pulumi.run { ctx ->
      val labels = mapOf("app" to "nginx")
      deploymentResource("nginx") {
         args {
            spec {
               selector {
                  matchLabels(labels)
               }
               replicas(1)
               template {
                  metadata {
                     labels(labels)
                  }
                  spec {
                     containers(
                        {
                           name("nginx")
                           image("nginx")
                           ports(
                              {
                                 containerPort(80)
                              },
                           )
                        },
                     )
                  }
               }
            }
         }
      }
   }
}
```

## Getting started

1. Create a new Pulumi Java project using the `pulumi new` command.
2. Authenticate to GitHub Packages as
   per [the docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package).
3. Replace the Java dependency in `pom.xml` with the corresponding Kotlin dependency (e.g.
   replace [Pulumi GCP](https://search.maven.org/artifact/com.pulumi/gcp/6.44.0/jar)
   with [Pulumi Kotlin GCP](https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1738521)).
4. Write your code or adapt a Java example into
   Kotlin ([here](https://github.com/VirtuslabRnD/pulumi-kotlin/blob/main/examples/gcp-sample-project/src/main/kotlin/project/Main.kt)
   you can find the adaptation of [this](https://www.pulumi.com/registry/packages/gcp/api-docs/compute/instance/) Java
   example).
5. Run or preview the project using the `pulumi up` and `pulumi preview` commands.

## Reference

### Supported providers

The table below lists the providers that are currently supported by Pulumi Kotlin. A full list of all Pulumi providers
can be found [here](https://www.pulumi.com/registry/). These Kotlin libraries serve as a wrapper on top of
corresponding Java libraries. They follow the same versioning pattern with an additional index, e.g. version `5.16.2.0`
of Pulumi AWS (Kotlin) is a wrapper on top of Pulumi AWS (Java) `5.16.2`. If the additional index is incremented (e.g.
to `5.16.2.1`), it means that some updates were made to our generator and that the Kotlin code has been improved, but
the underlying Java library remained the same.

| name            | Pulumi API docs                       | Kotlin package name                         | GitHub Packages repo                  | Kotlin docs                               | 
|-----------------|---------------------------------------|---------------------------------------------|---------------------------------------|-------------------------------------------|
| `alicloud`      | [link][pulumi-registry-alicloud]      | `com.virtuslab.pulumi-alicloud-kotlin`      | [link][github-packages-alicloud]      | [`3.28.0.0`][docs-alicloud-3.28.0.0]      |
| `aws`           | [link][pulumi-registry-aws]           | `com.virtuslab.pulumi-aws-kotlin`           | [link][github-packages-aws]           | [`5.16.2.0`][docs-aws-5.16.2.0]           |
| `aws-native`    | [link][pulumi-registry-aws-native]    | `com.virtuslab.pulumi-aws-native-kotlin`    | [link][github-packages-aws-native]    | [`0.42.0.0`][docs-aws-native-0.42.0.0]    |
| `azure`         | [link][pulumi-registry-azure]         | `com.virtuslab.pulumi-azure-kotlin`         | [link][github-packages-azure]         | [`5.24.0.0`][docs-azure-5.24.0.0]         |
| `azure-native`  | [link][pulumi-registry-azure-native]  | `com.virtuslab.pulumi-azure-native-kotlin`  | [link][github-packages-azure-native]  | [`1.87.0.0`][docs-azure-native-1.87.0.0]  |
| `cloudflare`    | [link][pulumi-registry-cloudflare]    | `com.virtuslab.pulumi-cloudflare-kotlin`    | [link][github-packages-cloudflare]    | [`4.12.1.0`][docs-cloudflare-4.12.1.0]    |
| `digitalocean`  | [link][pulumi-registry-digitalocean]  | `com.virtuslab.pulumi-digitalocean-kotlin`  | [link][github-packages-digitalocean]  | [`4.16.0.0`][docs-digitalocean-4.16.0.0]  |
| `docker`        | [link][pulumi-registry-docker]        | `com.virtuslab.pulumi-docker-kotlin`        | [link][github-packages-docker]        | [`3.5.0.0`][docs-docker-3.5.0.0]          |
| `gcp`           | [link][pulumi-registry-gcp]           | `com.virtuslab.pulumi-gcp-kotlin`           | [link][github-packages-gcp]           | [`6.43.0.0`][docs-gcp-6.43.0.0]           |
| `github`        | [link][pulumi-registry-github]        | `com.virtuslab.pulumi-github-kotlin`        | [link][github-packages-github]        | [`4.17.0.0`][docs-github-4.17.0.0]        |
| `gitlab`        | [link][pulumi-registry-gitlab]        | `com.virtuslab.pulumi-gitlab-kotlin`        | [link][github-packages-gitlab]        | [`4.9.0.0`][docs-gitlab-4.9.0.0]          |
| `google-native` | [link][pulumi-registry-google-native] | `com.virtuslab.pulumi-google-native-kotlin` | [link][github-packages-google-native] | [`0.27.0.0`][docs-google-native-0.27.0.0] |
| `kubernetes`    | [link][pulumi-registry-kubernetes]    | `com.virtuslab.pulumi-kubernetes-kotlin`    | [link][github-packages-kubernetes]    | [`3.22.1.0`][docs-kubernetes-3.22.1.0]    |
| `nomad`         | [link][pulumi-registry-nomad]         | `com.virtuslab.pulumi-nomad-kotlin`         | [link][github-packages-nomad]         | [`0.3.0.0`][docs-nomad-0.3.0.0]           |
| `random`        | [link][pulumi-registry-random]        | `com.virtuslab.pulumi-random-kotlin`        | [link][github-packages-random]        | [`4.6.0.0`][docs-random-4.6.0.0]          |
| `slack`         | [link][pulumi-registry-slack]         | `com.virtuslab.pulumi-slack-kotlin`         | [link][github-packages-slack]         | [`0.3.0.0`][docs-slack-0.3.0.0]           |

[pulumi-registry-alicloud]: https://www.pulumi.com/registry/packages/alicloud/api-docs/
[pulumi-registry-aws]: https://www.pulumi.com/registry/packages/aws/api-docs/
[pulumi-registry-aws-native]: https://www.pulumi.com/registry/packages/aws-native/api-docs/
[pulumi-registry-azure]: https://www.pulumi.com/registry/packages/azure/api-docs/
[pulumi-registry-azure-native]: https://www.pulumi.com/registry/packages/azure-native/api-docs/
[pulumi-registry-cloudflare]: https://www.pulumi.com/registry/packages/cloudflare/api-docs/
[pulumi-registry-digitalocean]: https://www.pulumi.com/registry/packages/digitalocean/api-docs/
[pulumi-registry-docker]: https://www.pulumi.com/registry/packages/docker/api-docs/
[pulumi-registry-gcp]: https://www.pulumi.com/registry/packages/gcp/api-docs/
[pulumi-registry-github]: https://www.pulumi.com/registry/packages/github/api-docs/
[pulumi-registry-gitlab]: https://www.pulumi.com/registry/packages/gitlab/api-docs/
[pulumi-registry-google-native]: https://www.pulumi.com/registry/packages/google-native/api-docs/
[pulumi-registry-kubernetes]: https://www.pulumi.com/registry/packages/kubernetes/api-docs/
[pulumi-registry-nomad]: https://www.pulumi.com/registry/packages/nomad/api-docs/
[pulumi-registry-random]: https://www.pulumi.com/registry/packages/random/api-docs/
[pulumi-registry-slack]: https://www.pulumi.com/registry/packages/slack/api-docs/

[//]: # (TODO: Add real links)
[github-packages-alicloud]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749160
[github-packages-aws]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749220
[github-packages-aws-native]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749163
[github-packages-azure]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749174
[github-packages-azure-native]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749226
[github-packages-cloudflare]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749153
[github-packages-digitalocean]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749151
[github-packages-docker]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749152
[github-packages-gcp]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749166
[github-packages-github]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749154
[github-packages-gitlab]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749156
[github-packages-google-native]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749215
[github-packages-kubernetes]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749161
[github-packages-nomad]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749157
[github-packages-random]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749158
[github-packages-slack]: https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1749159

[docs-alicloud-3.28.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/alicloud/3.28.0.0/index.html 
[docs-github-4.17.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/github/4.17.0.0/index.html
[docs-aws-5.16.2.0]: https://storage.googleapis.com/pulumi-kotlin-docs/aws/5.16.2.0/index.html
[docs-aws-native-0.42.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/aws-native/0.42.0.0/index.html
[docs-azure-5.24.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/azure/5.24.0.0/index.html
[docs-azure-native-1.87.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/azure-native/1.87.0.0/index.html
[docs-cloudflare-4.12.1.0]: https://storage.googleapis.com/pulumi-kotlin-docs/cloudflare/4.12.1.0/index.html
[docs-digitalocean-4.16.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/digitalocean/4.16.0.0/index.html
[docs-docker-3.5.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/docker/3.5.0.0/index.html
[docs-gcp-6.43.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/gcp/6.43.0.0/index.html
[docs-gitlab-4.9.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/gitlab/4.9.0.0/index.html
[docs-google-native-0.27.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/google-native/0.27.0.0/index.html
[docs-kubernetes-3.22.1.0]: https://storage.googleapis.com/pulumi-kotlin-docs/kubernetes/3.22.1.0/index.html
[docs-nomad-0.3.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/nomad/0.3.0.0/index.html
[docs-random-4.6.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/random/4.6.0.0/index.html
[docs-slack-0.3.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/slack/0.3.0.0/index.html

## Further reading

* [Code generation and local run details](docs/code-generation.md)
* [Development guidelines](docs/development-guidelines.md)
