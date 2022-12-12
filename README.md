# `pulumi-kotlin`

https://user-images.githubusercontent.com/4415632/192312941-d4893fe1-c896-45c6-84aa-c9e5c3523a9a.mp4

Experimental support for Kotlin language in Pulumi.

**Work in progress, expect chaos and terrible code.**

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

| name            | Pulumi API docs                       | Kotlin package name                         | GitHub Packages repo | Kotlin docs                        | 
|-----------------|---------------------------------------|---------------------------------------------|----------------------|------------------------------------|
| `alicloud`      | [link][pulumi-registry-alicloud]      | `com.virtuslab.pulumi-alicloud-kotlin`      | [link](TODO)         | [`3.28.0.0`]()                     |
| `aws`           | [link][pulumi-registry-aws]           | `com.virtuslab.pulumi-aws-kotlin`           | [link](TODO)         | [`5.16.2.0`]()                     |
| `aws-native`    | [link][pulumi-registry-aws-native]    | `com.virtuslab.pulumi-aws-native-kotlin`    | [link](TODO)         | [`0.42.0.0`]()                     |
| `azure`         | [link][pulumi-registry-azure]         | `com.virtuslab.pulumi-azure-kotlin`         | [link](TODO)         | [`5.24.0.0`]()                     |
| `azure-natice`  | [link][pulumi-registry-azure-native]  | `com.virtuslab.pulumi-azure-native-kotlin`  | [link](TODO)         | [`1.87.0.0`]()                     |
| `cloudflare`    | [link][pulumi-registry-cloudflare]    | `com.virtuslab.pulumi-cloudflare-kotlin`    | [link](TODO)         | [`4.12.1.0`]()                     |
| `digitalocean`  | [link][pulumi-registry-digitalocean]  | `com.virtuslab.pulumi-digitalocean-kotlin`  | [link](TODO)         | [`4.16.0.0`]()                     |
| `docker`        | [link][pulumi-registry-docker]        | `com.virtuslab.pulumi-docker-kotlin`        | [link](TODO)         | [`3.5.0.0`]()                      |
| `gcp`           | [link][pulumi-registry-gcp]           | `com.virtuslab.pulumi-gcp-kotlin`           | [link](TODO)         | [`6.43.0.0`]()                     |
| `github`        | [link][pulumi-registry-github]        | `com.virtuslab.pulumi-github-kotlin`        | [link](TODO)         | [`4.17.0.0`][docs-github-4.17.0.0] |
| `gitlab`        | [link][pulumi-registry-gitlab]        | `com.virtuslab.pulumi-gitlab-kotlin`        | [link](TODO)         | [`4.9.0.0`]()                      |
| `google-native` | [link][pulumi-registry-google-native] | `com.virtuslab.pulumi-google-native-kotlin` | [link](TODO)         | [`0.27.0.0`]()                     |
| `kubernetes`    | [link][pulumi-registry-kubernetes]    | `com.virtuslab.pulumi-kubernetes-kotlin`    | [link](TODO)         | [`3.22.1.0`]()                     |
| `nomad`         | [link][pulumi-registry-nomad]         | `com.virtuslab.pulumi-nomad-kotlin`         | [link](TODO)         | [`0.3.0.0`]()                      |
| `random`        | [link][pulumi-registry-random]        | `com.virtuslab.pulumi-random-kotlin`        | [link](TODO)         | [`4.6.0.0`]()                      |
| `slack`         | [link][pulumi-registry-slack]         | `com.virtuslab.pulumi-slack-kotlin`         | [link](TODO)         | [`0.3.0.0`]()                      |

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

[docs-github-4.17.0.0]: https://storage.googleapis.com/pulumi-kotlin-docs/github/4.17.0.0/index.html

## Development

### Releasing

All schema versions used for releasing the libraries are configured in the `src/main/resources/version-config.json`
file. There are two release paths:

1. If you want update provider schemas and build new versions of the libraries, run the Gradle
   task `prepareReleaseOfUpdatedSchemas`.
2. If you want to release a new version of all libraries due to some update in the generator (i.e. the `pulumi-kotlin`
   codebase), run the Gradle task `prepareReleaseAfterGeneratorUpdate`.

These tasks will create a new commit which updates the versions in `version-config.json` accordingly. Push this commit
out to a new branch and create a PR (like [this one](https://github.com/VirtuslabRnD/pulumi-kotlin/pull/98)). Once this
PR is approved and merged, a GitHub Action will be triggered
(like [this one](https://github.com/VirtuslabRnD/pulumi-kotlin/actions/runs/3328060887)), which will:

1. Tag the merge commit with the appropriate release versions.
2. Release the requested libraries to the
   [Maven registry (hosted on GitHub)](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).
   The released libraries will be visible in the
   [Packages](https://github.com/orgs/VirtuslabRnD/packages?repo_name=pulumi-kotlin) section on GitHub.
3. Fast-forward the released versions to SNAPSHOT versions and create a PR (
   like [this one](https://github.com/VirtuslabRnD/pulumi-kotlin/pull/99)). Once this PR is approved and merged, the
   release cycle is complete.

In the future, the task `prepareReleaseOfUpdatedSchemas` could be run automatically as a cron job. For now, it will need
to be run manually by one of the team members.
