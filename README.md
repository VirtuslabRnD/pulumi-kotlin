# `pulumi-kotlin`

https://user-images.githubusercontent.com/4415632/192312941-d4893fe1-c896-45c6-84aa-c9e5c3523a9a.mp4

Experimental support for Kotlin language in Pulumi.

**Work in progress, expect chaos and terrible code.**

## How to run this locally?

There are two ways:

- Run from command line (Gradle).
    - Example: `./gradlew run --args='--schema-path src/main/resources/schema-aws-classic-subset-for-build.json --output-directory-path output-dir'`.
- Run from IntelliJ with `Program arguments` properly set.

### Arguments

⚠️ Note: *Use `src/main/resources/schema-aws-classic.json` schema for now. Google or Azure won't work yet.*

- `--schema-path` – path to provider's schema (see section below for how to download it).
- `--output-directory-path` – path to a directory where the generated code should be stored.

## Schema

### How to download provider's schema?

#### Directly from the repository

Schema is stored in provider's repository, under `provider/cmd/pulumi-resource-{{provider-name}}` path.

For example, to download `pulumi-aws-native`'s schema, open this [link][pulumi-aws-native-schema-link]
and save the file (Right click 'Download' -> 'Save link as...').

#### Through `pulumi` CLI

⚠️ Warning: This is not recommended ([issue][slash-encoding-issue]). Schemas downloaded this way have `/` encoded
as `%2F` (in `$ref`).

Use the `pulumi package get-schema` command, for example:

```bash
pulumi package get-schema 'azure-native' > azure-native-schema.json
```

### How to compute a subset of some schema?

Run `gradlew computeSchemaSubset` from Gradle or `ComputeSchemaSubsetScript.kt` from IntelliJ.

Example:

```bash
./gradlew computeSchemaSubset --args="--schema-path=./gcp-schema.json --name=gcp:compute/instance:Instance --context=resource"
```

## How does it work under the hood?

⚠️ Note: *Remember, this repo is proof of concept, code is not of highest quality and things can quickly change.*

1. The provided schema is deserialized (`step1schemaparse`).
1. The deserialized schema is converted into intermediate representation – graph of type
   dependencies (`step2intermediate`).
1. The intermediate representation is used to generate Kotlin code (`step3codegen`).

    - Code is generated with [KotlinPoet](https://github.com/square/kotlinpoet).
    - The generated SDK allows to write an idiomatic Kotlin code
      (see [type-safe builders](https://kotlinlang.org/docs/type-safe-builders.html)).
    - The generated SDK delegates all the work to Pulumi Java SDK.

## What is possible with Kotlin SDK? (code examples)

```kotlin
import com.pulumi.aws.acm.kotlin.certificateResource

suspend fun main() {
    val resource1 = certificateResource("name") {
        args {
            subjectAlternativeNames("one", "two")
            validationOptions(
                {
                    domainName("whatever")
                    validationDomain("whatever")
                },
                {
                    domainName("whatever2")
                    validationDomain("whatever2")
                }
            )
            options {
                certificateTransparencyLoggingPreference("test")
            }
        }
        opts {
            protect(true)
            retainOnDelete(false)
            ignoreChanges(listOf("validationOptions"))
        }
    }

    val resource2 = certificateResource("name2") {
        args {
            subjectAlternativeNames(resource1.status.applyValue { listOf(it) })
            validationOptions(
                {
                    domainName(resource1.status)
                    validationDomain("whatever")
                }
            )
            options {
                certificateTransparencyLoggingPreference("test")
            }
        }
        opts {
            retainOnDelete(true)
            ignoreChanges(listOf("validationOptions"))
        }
    }
}
```

## Development

Development guidelines can be
found [here](https://github.com/VirtuslabRnD/jvm-lab/blob/main/README.md#development-standards-for-kotlin-repositories).

[slash-encoding-issue]: https://github.com/VirtuslabRnD/pulumi-kotlin/issues/87

[pulumi-aws-native-schema-link]: https://github.com/pulumi/pulumi-aws-native/blob/master/provider/cmd/pulumi-resource-aws-native/schema.json

## Releasing

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
