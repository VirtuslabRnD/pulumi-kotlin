# `pulumi-kotlin`

## How to run code generation locally?

There are two ways:

* run from command line (Gradle)
  * example:
    ```bash
    ./gradlew run --args="--schema-path $HOME/Workspaces/pulumi/schemas/gcp-classic_6.4.0.json --output-directory-path $HOME/Workspaces/generated/pulumi-kotlin-generated/app/src/main/kotlin"
    ```
* run from IntelliJ with `Program arguments` properly set

In order to easily browse generated files one can generate them directly into basic gradle project,  
hence `--output-directory-path $HOME/Workspaces/generated/pulumi-kotlin-generated/app/src/main/kotlin`

### Arguments

* `--schema-path` – absolute path to provider's schema (see section below for how to download it)
* `--output-directory-path` – absolute path to a directory where the generated code should be stored

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

```sh
pulumi package get-schema 'azure-native' > azure-native-schema.json
```

### How to compute a subset of some schema?

Code generation and compilation of large schemas can take time,  
in order to test only particular resources one can prepare schema subset with `ComputeSchemaSubsetScript.kt`
and generate Kotlin SDK out of it.  

This script will generate subset with all types referenced by sought-after object.

`ComputeSchemaSubsetScript.kt` can be run in two ways:
* from command line:
  ```bash
  ./gradlew computeSchemaSubset --args="--schema-path $HOME/Workspaces/pulumi/schemas/gcp-classic_6.4.0.json --name gcp:compute/instance:Instance --context resource --output-path $HOME/Workspaces/pulumi/schemas/gcp-classic_6.4.0_instance.json"
  ```
  * arguments
    * `--full-schema-path` - absolute path to full provider's schema
    * `--existing-schema-subset-path` - (optional) absolute path to existing schema subset, objects present there will be contained in result subset
    * `--output-path` - (optional) script will save subset schema's contents in this path, if it's not given, schema will be printed to `stdout`
    * `--name-and-context` - name and kind of object to find in the schema, i.e. `gcp:compute/instance:Instance resource`,  
      possible `context` values:
      * `function`
      * `type`
      * `resource`
      * `provider`  
      this parameter can be passed multiple times
    * `--shorten-descriptions` - (optional) when `true` reduces `description` property length to 100 characters, default `false`
    * `--load-provider-with-children` - (optional) when `true`, considers also provider resource in resulting schema along with types it depends on, default `true`
    * `--load-full-parents` - (optional) when `true` considers objects referencing to object given as value of `--name-and-context`, default `false`
* from IntelliJ with `Program arguments` properly set (possible properties and values like above)

## How does it work under the hood?

1. The provided schema is deserialized (`step1schemaparse`).
2. The deserialized schema is converted into intermediate representation – graph of type
   dependencies (`step2intermediate`).
3. The intermediate representation is used to generate Kotlin code (`step3codegen`).
    * Code is generated with [KotlinPoet][kotlin-poet].
    * The generated SDK allows to write an idiomatic Kotlin code
      (see [type-safe builders][type-safe-builders]).
    * The generated SDK delegates all the work to Pulumi Java SDK.

[pulumi-aws-native-schema-link]: https://github.com/pulumi/pulumi-aws-native/raw/master/provider/cmd/pulumi-resource-aws-native/schema.json
[slash-encoding-issue]: https://github.com/VirtuslabRnD/pulumi-kotlin/issues/87

[kotlin-poet]: https://github.com/square/kotlinpoet
[type-safe-builders]: https://kotlinlang.org/docs/type-safe-builders.html
