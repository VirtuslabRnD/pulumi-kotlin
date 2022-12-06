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
2. Replace the Java dependency in `pom.xml` with the corresponding Kotlin dependency (e.g.
   replace [Pulumi GCP](https://search.maven.org/artifact/com.pulumi/gcp/6.44.0/jar)
   with [Pulumi Kotlin GCP](https://github.com/VirtuslabRnD/pulumi-kotlin/packages/1738521)). Currently, Pulumi Kotlin
   libraries are published only on GitHub Packages, which means that you have to authenticate to GitHub in order to
   attach the dependency (see:
   [docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package)).
3. Write your code or adapt a Java example into
   Kotlin ([here](https://github.com/VirtuslabRnD/pulumi-kotlin/blob/main/examples/gcp-sample-project/src/main/kotlin/project/Main.kt)
   you can find the adaptation of [this](https://www.pulumi.com/registry/packages/gcp/api-docs/compute/instance/) Java
   example).
4. Run or preview the project using the `pulumi up` and `pulumi preview` commands.
: https://github.com/pulumi/pulumi-aws-native/blob/master/provider/cmd/pulumi-resource-aws-native/schema.json

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
