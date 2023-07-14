# Kubernetes sample project

This project creates an Nginx deployment using the Pulumi Kotlin Kubernetes SDK.

## How to run

**See the
[Kubernetes: Installation & Configuration](https://www.pulumi.com/registry/packages/kubernetes/installation-configuration/)
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
3. Provision a Kubernetes cluster (for test purposes this can be done
   using [Minikube](https://minikube.sigs.k8s.io/docs/start/)).
4. Install kubectl (see: [docs](https://kubernetes.io/docs/tasks/tools/)).
   ```bash
   # on MacOS
   brew install kubectl
   ```
5. Set up Kubernetes authentication
   (see: [docs](https://www.pulumi.com/registry/packages/kubernetes/installation-configuration/#setup)). **Note**: when
   using Minikube, you can use the default configuration and skip this step.
6. Create a context. **Note**: when using Minikube, you can use the default `minikube` context and skip this step.
   ```bash
   kubectl config \
    set-context <my-context> \
    --cluster=<my-cluster> \
    --user=<my-user>
   ```
7. Set the necessary Pulumi configuration properties. **Note**: when using Minikube, the default context is
   called `minikube`.
   ```bash
   pulumi config set kubernetes:context <my-context>
   ```
8. Create a stack and perform the update.
   ```bash
   pulumi up
   ```
9. Don't forget to delete the created resources and stack.
   ```bash
   pulumi destroy
   pulumi stack rm <stack-name>
   ```
