# kubernetes-manager
src/backend/dispatch-k8s-manager
## 开发须知

1. 修改resource下的config文件时需要同步修改 manifests中的configmap，保持一致。
2. 修改接口后，需要运行 ./swagger/init-swager.sh 重新初始化swagger文档。

## 使用须知

kubernetes-manager可以使用二进制方式启动，也可以使用容器方式（更加推荐作为容器启动）。

### 以容器方式启动

1. 打包镜像。通过修改 makefile 中的 LOCAL_REGISTR与LOCAL_IMAGE，修改默认镜像参数后 make -f ./Makefile image.xxx 打包自己需要的架构。或者直接使用docker文件夹下Dockerfile参考makefile中命令自行打包。打包后即可作为docker容器使用（需配合现有的redis和mysql）。

2. 打包chart。通过修改manifests/chart 的Chart.yaml 信息，通过 helm package打包即可。启动时通过阅读并修改values中的内容定制自己需要的启动配置即可（chart包中默认携带mysql以及redis，不需要可以关闭）。

3. 补充说明：
    - **如何链接不同的kubernetes集群**通过修改 values中的 useKubeConfig 参数即可开启使用指定的kubeconfig，同时修改 chart/template/kubernetes-manager-configmap.yaml 中 kubeConfig.yaml 即可。
    - **登录调试相关** 因为登录调试需要将https链接转为wss与kuberntes通信，所以需要 **指定需要登录调试集群的kubeconfig**，指定方式参考 **如何链接不同的kubernetes集群**。
    - **realResource优化** 优化使用了kubernetes-scheduler-pluign和prometheus的特性，所以需要配置 prometheus同时需要安装 [ci-dispatch-k8s-manager-plugin](https://github.com/TencentBlueKing/ci-dispatch-k8s-manager-plugin) 插件。
### 部署方案
#### 【构建机】和【蓝盾服务】同k8s集群同namespace部署(bk-ci默认部署方式)
‼️禁止在生产环境采用这种部署方式
#### 【构建机】和【蓝盾服务】同集群不同namespace部署(基本的安全隔离)
1. 创建构建机namespace , 如: devops-build
2. 配置bk-ci helm values
```
kubernetes-manager:
    kubernetesManager:
        builderNamespace: devops-build // 构建机的namespace
config:
    bkCiPrivateUrl: {{ 蓝盾可访问域名 }} // 如: devops.example.com
```
#### 【构建机】和【蓝盾服务】不同集群部署(最安全,网络隔离)
1. 独立部署kubernetes-manager, 将[kubernetes-manager](https://github.com/TencentBlueKing/bk-ci/tree/master/helm-charts/core/ci/local_chart/kubernetes-management) 下载下来 , 配置values.yaml后进行helm install
2. 配置bk-ci helm values
```
kubernetes-manager:
    enabled: false // bk-ci部署的时候不带上kubernetes-manager
config:
    bkCiPrivateUrl: {{ 蓝盾可访问域名 }} // 如: devops.example.com
    bkCiKubernetesHost: {{ kubernetes-manager可访问域名 }}
```
3. **必须同步身份签名密钥。** 跨集群时 dispatch 读不到 manager 的 Secret（`optional: true` 会静默留空）。把同一份 `identitySigningKey` 写到 manager 与 dispatch，否则已属主构建机无法回收。详见下方「安全配置项」。


### 安全配置项（#13571 新增，部署必读）

Helm 同 namespace 默认安装会创建 Secret `kubernetes-manager-auth`，首装 `randAlphaNum 32`，升级 `lookup` 保值。**不要**把仓库里曾经出现过的公开串写进 values：

- `bkci-k8s-manager-debug-ticket-change-in-prod`
- `bkci-k8s-manager-identity-sig-change-in-prod`

写入也会被当成未配置并重新生成或直接拒绝。

| 配置项 | 注入位置 | 留空 / 填公开串会发生什么 |
|--------|----------|---------------------------|
| `apiserver.auth.debugTicketSecret` | manager env `K8S_MANAGER_DEBUG_TICKET_SECRET` | `/terminal`、`/debug` 直接 503，远程登录不可用。不是越权面，但是功能关闭。 |
| `apiserver.auth.identitySigningKey` | manager env `K8S_MANAGER_IDENTITY_SIGNING_KEY` | 自称身份头全部丢弃。已打属主的构建机 stop/delete/start 会 403，池位无法回收。 |
| `kubernetes.identitySigningKey` | dispatch env `KUBERNETES_IDENTITY_SIGNING_KEY` | dispatch 不发 SIG，效果同上。Spring 从该 env 绑到 `kubernetes.identitySigningKey`。 |

**同 namespace（默认）：** dispatch Deployment 已 `secretKeyRef` 共读同一 Secret，无需人工复制。

**跨 namespace / 跨集群：** `optional: true`，Secret 不在本 ns 时 env 为空，属于静默降级。必须把同一 `identitySigningKey` 显式配到 dispatch（env 或 `application-dispatch.yml`），并保证 manager 侧一致。GitOps 反复 apply 时建议把两个 key **显式固定**，不要依赖每次 `randAlphaNum`。

身份签名 payload 为 `uid|pid|tid|ts|METHOD|path`，窗口 60 秒，两侧需 NTP。manager 启动时若身份密钥未配置会打 Warn。

禁止把上述密钥注入构建容器。共享 `Devops-Token`（默认 `landun`）只能证明内部调用，不能替代身份签名。

### 以二进制的方式启动

1. 打包二进制。参考makefile中的 build.xxx 和 release.xxx 同时修改makefile中 CONFIG_DIR，OUT_DIR来存放配置文件和目录文件（配置文件格式可参考 resources 目录）。

2. 补充说明：
    - 二进制格式启动类似直接镜像启动，可以相互参考。同时二进制格式启动一样不具备mysql和redis，需要自行准备。
