# kubernetes-manager
src/backend/dispatch-k8s-manager
## Development Notes

1. When modifying config files under the resource directory, ensure to synchronize changes with the configmap in manifests to maintain consistency.
2. After modifying APIs, run ./swagger/init-swager.sh to regenerate the swagger documentation.

## Usage Guide

kubernetes-manager can be started as a binary or as a container (recommended for containerized deployment).

### Container Deployment

1. Build the image:
   - Modify LOCAL_REGISTRY and LOCAL_IMAGE in the Makefile
   - Run `make -f ./Makefile image.xxx` for target architecture
   - Alternatively, use the Dockerfile in the docker directory with manual build commands

2. Package the Helm chart:
   - Modify Chart.yaml in manifests/chart
   - Run `helm package`
   - Customize startup configuration via values.yaml (default includes MySQL/Redis - disable if unused)

3. Additional notes:
   - **Multi-cluster support**: Enable useKubeConfig in values.yaml and update kubeConfig.yaml in kubernetes-manager-configmap.yaml
   - **Login debugging**: Requires target cluster's kubeconfig (see multi-cluster setup)
   - **realResource optimization**: Requires Prometheus and [ci-dispatch-k8s-manager-plugin](https://github.com/TencentBlueKing/ci-dispatch-k8s-manager-plugin)

### Deployment Scenarios
#### Same cluster/namespace (default)
⚠️ Not recommended for production
#### Same cluster/different namespaces (basic isolation)
1. Create builder namespace (e.g., devops-build)
2. Configure bk-ci helm values:
```yaml
kubernetes-manager:
  kubernetesManager:
    builderNamespace: devops-build
config:
  bkCiPrivateUrl: {{ your-domain }}  # e.g., devops.example.com
```
#### Cross-cluster deployment (most secure)
1. Deploy kubernetes-manager independently using [this chart](https://github.com/TencentBlueKing/bk-ci/tree/master/helm-charts/core/ci/local_chart/kubernetes-management)
2. Configure bk-ci helm values:
```yaml
kubernetes-manager:
  enabled: false
config:
  bkCiPrivateUrl: {{ your-domain }}
  bkCiKubernetesHost: {{ manager-domain }}
```

### Binary Deployment
1. Build binaries:
   - Reference build.xxx/release.xxx in Makefile
   - Set CONFIG_DIR and OUT_DIR for config/output paths
2. Requirements:
   - External MySQL/Redis required
   - Start command: `./kubernetes-manager --config=config.yaml`
