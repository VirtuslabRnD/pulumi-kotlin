# Google Native sample project

This project creates a VM instance in Google Cloud using the Pulumi Kotlin GCP native SDK.

## How to run

**See the
[Google Cloud Native: Installation & Configuration](https://www.pulumi.com/registry/packages/google-native/installation-configuration/)
page in Pulumi Registry for the official documentation.**

1. Install Pulumi (see: [docs](https://www.pulumi.com/docs/install/)).
   ```bash
   # on MacOS
   brew install pulumi/tap/pulumi
   ```
2. Log in to your Pulumi account (see: [docs](https://www.pulumi.com/docs/cli/commands/pulumi_login/)).
   ```bash
   pulumi login
   ```
3. Install the gcloud CLI (see: [docs](https://cloud.google.com/sdk/docs/install)).
   ```bash
   # on MacOS
   brew install google-cloud-sdk
   ```
4. Authenticate to GCP
   (see: [docs](https://www.pulumi.com/registry/packages/google-native/installation-configuration/#configuration)).
   ```bash
   gcloud auth login
   ```
5. Set the necessary Pulumi configuration properties.
   ```bash
   pulumi config set gcp:project <your-gcp-project-id>
   ```   
6. Create a stack and perform the update.
   ```bash
   pulumi up
   ```
7. Don't forget to delete the created resources and stack.
   ```bash
   pulumi destroy
   pulumi stack rm <stack-name>
   ```
