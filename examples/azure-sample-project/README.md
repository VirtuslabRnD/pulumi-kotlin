# Azure sample project

This project creates a VM instance in Azure Cloud using the Pulumi Kotlin Azure classic SDK.

## How to run

1. Install Pulumi (see: [docs](https://www.pulumi.com/docs/install/)).
2. Log in to your Pulumi account (see: [docs](https://www.pulumi.com/docs/cli/commands/pulumi_login/)).
   ```bash
   pulumi login
   ```
3. Install the Azure CLI (see: [docs](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)).
4. Authenticate to Azure (see: 
   [docs](https://www.pulumi.com/registry/packages/azure/installation-configuration/#authentication-methods)).
   ```bash
   az login
   ```
5. Set the necessary Pulumi configuration properties.
   ```bash
   pulumi config set azure-native:location westeurope 
   ```
6. Create a stack and perform the update.
   ```bash
   pulumi up
   ```
7. Don't forget to delete the created resources and stack.
   ```bash
   pulumi down
   pulumi stack rm <stack-name>
   ```
