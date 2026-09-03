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
3. **Sync the identity signing key.** Dispatch cannot read manager's Secret across clusters (`optional: true` leaves the env empty). Set the same `identitySigningKey` on both sides or owned builders cannot be reclaimed. See Security settings below.

### Security settings (required after #13571)

Same-namespace Helm creates Secret `kubernetes-manager-auth` (`randAlphaNum 32` on first install, `lookup` on upgrade). Never put these published strings in values — they are reject-listed and treated as unset:

- `bkci-k8s-manager-debug-ticket-change-in-prod`
- `bkci-k8s-manager-identity-sig-change-in-prod`

| Key | Injected as | If empty or published default |
|-----|-------------|-------------------------------|
| `apiserver.auth.debugTicketSecret` | manager `K8S_MANAGER_DEBUG_TICKET_SECRET` | `/terminal` and `/debug` return 503. Login debug is off; this is availability, not a privilege bypass. |
| `apiserver.auth.identitySigningKey` | manager `K8S_MANAGER_IDENTITY_SIGNING_KEY` | Identity headers are dropped. stop/delete/start on owned builders return 403; pool slots leak. |
| `kubernetes.identitySigningKey` | dispatch `KUBERNETES_IDENTITY_SIGNING_KEY` | Dispatch sends no SIG. Same 403 / pool-leak outcome. |

**Same namespace:** dispatch already mounts the same Secret via `secretKeyRef`. Do not copy by hand.

**Cross-namespace / cross-cluster:** `optional: true` leaves the env empty if the Secret is missing. You must set the same `identitySigningKey` on dispatch and manager. For GitOps, pin both keys explicitly.

Identity HMAC payload is `uid|pid|tid|ts|METHOD|path` with a 60s window (NTP required). Manager logs a Warn if the identity key is unset. Never inject these keys into builder pods. Shared `Devops-Token` is not a substitute for identity signatures.

### Binary Deployment
1. Build binaries:
   - Reference build.xxx/release.xxx in Makefile
   - Set CONFIG_DIR and OUT_DIR for config/output paths
2. Requirements:
   - External MySQL/Redis required
   - Start command: `./kubernetes-manager --config=config.yaml`
