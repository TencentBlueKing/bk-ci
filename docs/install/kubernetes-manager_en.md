# kubernetes-manager
src/backend/dispatch-k8s-manager
## Development Notes

1. When modifying config files under the resource directory, ensure to synchronize changes with the configmap in manifests to maintain consistency.
2. After modifying APIs, run ./swagger/init-swager.sh to regenerate the swagger documentation.

## Usage Notes

kubernetes-manager can be started as a binary or as a container (recommended for containerized deployment).

### Starting as a Container

1. Build the image. Modify LOCAL_REGISTRY and LOCAL_IMAGE in the Makefile, then run `make -f ./Makefile image.xxx` to build the required architecture. Alternatively, use the Dockerfile in the docker directory and follow the commands in the Makefile to build manually. Once built, it can be used as a Docker container (requires existing Redis and MySQL).

2. Package the chart. Modify Chart.yaml in manifests/chart, then package it using `helm package`. Customize the startup configuration by editing the values.yaml file (the chart includes MySQL and Redis by default, which can be disabled if not needed).

3. Additional Notes:
    - **Connecting to Different Kubernetes Clusters**: Enable the use of a specific kubeconfig by modifying the `useKubeConfig` parameter in values.yaml and updating `kubeConfig.yaml` in chart/template/kubernetes-manager-configmap.yaml.
    - **Login Debugging**: Since HTTPS links need to be converted to WSS for communication with Kubernetes, specify the kubeconfig for the cluster requiring login debugging (refer to **Connecting to Different Kubernetes Clusters**).
    - **realResource Optimization**: This leverages features from kubernetes-scheduler-plugin and Prometheus, requiring Prometheus configuration and installation of the [ci-dispatch-k8s-manager-plugin](https://github.com/TencentBlueKing/ci-dispatch-k8s-manager-plugin).

#### Deploying kubernetes-manager and bk-ci in the Same K8s Cluster and Namespace (Default Deployment)
Configure bk-ci helm values (default settings):
'bkCiKubernetesHost': "http://kubernetes-manager"  // Default service type is NodePort
'bkCiKubernetesToken': "landun" // Matches kubernetesManager.apiserver.auth.apiToken.value

#### Deploying kubernetes-manager and bk-ci in the Same Cluster but Different Namespaces
Configure bk-ci helm values:
'bkCiKubernetesHost': "http://kubernetes-manager.{{ .Release.Name }}"  // Default service type is NodePort
'bkCiKubernetesToken': "landun" // Matches kubernetesManager.apiserver.auth.apiToken.value

#### Deploying kubernetes-manager and bk-ci in Different Clusters
Configure bk-ci helm values:
'bkCiKubernetesHost': "http://node:port"  // Default service type is NodePort
'bkCiKubernetesToken': "landun" // Matches kubernetesManager.apiserver.auth.apiToken.value

### Starting as a Binary

1. Build the binary. Refer to `build.xxx` and `release.xxx` in the Makefile, and modify CONFIG_DIR and OUT_DIR to specify configuration and output directories (configuration files can be referenced from the resources directory).

2. Additional Notes:
    - Binary startup is similar to container startup and can be cross-referenced. Binary startup also requires separate MySQL and Redis setups.
