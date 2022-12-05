# CodeCC

此Chart用于在Kubernetes集群中通过helm部署Codecc

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 安装Chart
使用以下命令安装名称为`codecc`的release, 其中`<codecc helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <codecc helm repo url>
$ helm install codecc bkee/codecc
```

上述命令将使用默认配置在Kubernetes集群中部署codecc, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`codecc`:

```shell
$ helm uninstall codecc
```

上述命令将移除所有和codecc相关的Kubernetes组件，并删除release。

## Chart依赖
- [bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/nginx-ingress-controller)
- [bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

## 配置说明
下面展示了可配置的参数列表以及默认值

### Charts 全局设置

|参数|描述|默认值 |
|---|---|---|
| `global.imageRegistry`    | Global Docker image registry                    | `nil`                                                   |
| `global.imagePullSecrets` | Global Docker registry secret names as an array | `[]` (does not add image pull secrets to deployed pods) |
| `global.storageClass`     | Global storage class for dynamic provisioning   | `nil`                                                   |

### Kubernetes组件公共配置

下列参数用于配置Kubernetes组件的公共属性，一份配置作用到每个组件

|参数|描述|默认值 |
|---|---|---|
| `commonAnnotations` | Annotations to add to all deployed objects | `{}` |
| `commonLabels` | Labels to add to all deployed objects | `{}` |

### Kubernetes组件通用配置

下列参数表示Kubernetes组件的通用配置，每个微服务进行单独配置。能够配置的微服务有:

- gateway
- task
- asyncreport
- defect
- report

|参数|描述|默认值 |
|---|---|---|
| `image.registry`    | 镜像仓库              | `mirrors.tencent.com` |
| `image.repository`  | 镜像名称              | `bkce/codecc/xxx`          |
| `image.tag`         | 镜像tag               | `{TAG_NAME}`         |
| `image.pullPolicy`  | 镜像拉取策略           | `IfNotPresent`        |
| `image.pullSecrets` | 镜像拉取Secret名称数组  | `[]`                  |
| `imagePullSecrets`  | 镜像拉取secret名称列表  | `[]`                  |
| `securityContext`   | 容器 Security Context | `{}`                  |
| `replicaCount`                       | Number of pod replicas                                                                      | `2`                                                     |
| `hostAliases`                        | Add deployment host aliases                                                                       | `[]`                                                    |
| `resources.limits`                   | The resources limits for containers                                                          | `{}`                                                    |
| `resources.requests`                 | The requested resources for containers                                                       | `{}`                                                    |
| `affinity`                           | Affinity for pod assignment (evaluated as a template)                                                                   | `{}`                           |
| `containerSecurityContext.enabled`      | Enable containers' Security Context                                                                             | `false`                                      |
| `containerSecurityContext.runAsUser`    | Containers' Security Context                                                                                    | `1001`                                      |
| `containerSecurityContext.runAsNonRoot` | Containers' Security Context Non Root                                                                           | `true`                                      |
| `nodeAffinityPreset.key`             | Node label key to match Ignored if `affinity` is set.                                                                   | `""`                           |
| `nodeAffinityPreset.type`            | Node affinity preset type. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                               | `""`                           |
| `nodeAffinityPreset.values`          | Node label values to match. Ignored if `affinity` is set.                                                               | `[]`                           |
| `nodeSelector`                       | Node labels for pod assignment                                                                                          | `{}` (evaluated as a template) |
| `podLabels`                             | Add additional labels to the pod (evaluated as a template)                                                            | `nil`                                       |
| `podAnnotations`                     | Pod annotations                                                                                                         | `{}` (evaluated as a template) |
| `podAffinityPreset`                  | Pod affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                     | `""`                           |
| `podAntiAffinityPreset`              | Pod anti-affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                | `soft`                         |
| `podSecurityContext.enabled`         | Enable pod security context                                                                                             | `true`                         |
| `podSecurityContext.fsGroup`         | fsGroup ID for the pod                                                                                                  | `1001`                         |
| `priorityClassName`                     | Define the priority class name for the pod.                                                        | `""`                                        |
| `tolerations`                        | Tolerations for pod assignment                                                                                          | `[]` (evaluated as a template) |

### RBAC配置

|参数|描述|默认值 |
|---|---|---|
| `rbac.create`                        | If true, create & use RBAC resources                                                                                    | `true`                        |
| `serviceAccount.annotations`         | Annotations for service account                                                                                         | `{}`                           |
| `serviceAccount.create`              | If true, create a service account                                                                                       | `false`                        |
| `serviceAccount.name`                | The name of the service account to use. If not set and create is true, a name is generated using the fullname template. | ``                             |

### ingress 配置

|参数|描述|默认值 |
|---|---|---|
| `ingress.enabled` | 是否创建ingress | `true` |
| `annotations` | ingress标注 | Check `values.yaml` |

### nginx-ingress-controller 配置

默认将部署`nginx-ingress-controller`，如果不需要可以关闭。
相关配置请参考[bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/)

|参数|描述|默认值 |
|---|---|---|
| `nginx-ingress-controller.enabled` | 是否部署nginx ingress controller | `true` |
| `nginx-ingress-controller.defaultBackend.enabled` | nginx ingress controller默认backend | `false` |

### mongodb 配置
默认将部署mongodb，如果不需要可以关闭。
相关配置请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

|参数|描述|默认值 |
|---|---|---|
| `mongodb.enabled` | 是否部署mognodb。如果需要使用外部数据库，设置为`false`并配置`externalMongodb` | `true` |
| `externalMongodb.defectUrl` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，codecc将使用此参数连接外部mongodb | `mongodb://codecc:codecc@localhost:27017/db_defect` |
| `externalMongodb.taskUrl` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，codecc将使用此参数连接外部mongodb | `mongodb://codecc:codecc@localhost:27017/db_task` |

> 如果需要持久化mongodb数据，请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)配置存储卷

### 数据持久化配置

数据持久化配置, 当使用filesystem方式存储时需要配置。

|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled` | 是否开启数据持久化，false则使用emptyDir类型volume, pod结束后数据将被清空，无法持久化 | `true` |
| `persistence.accessMode` | PVC Access Mode for codecc data volume | `ReadWriteOnce` |
| `persistence.size` | PVC Storage Request for codecc data volume | `100Gi` |
| `persistence.storageClass` | 指定storageClass。如果设置为"-", 则禁用动态卷供应; 如果不设置, 将使用默认的storageClass(minikube上是standard) | `nil` |
| `persistence.existingClaim` | 如果开启持久化并且定义了该项，则绑定k8s集群中已存在的pvc | `nil` |

> 如果开启数据持久化，并且没有配置`existingClaim`，将使用[动态卷供应](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)提供存储，使用`storageClass`定义的存储类。**在删除该声明后，这个卷也会被销毁(用于单节点环境，生产环境不推荐)。**。

### codecc公共配置

|参数|描述|默认值 |
|---|---|---|
| `common.jvmOption` | jvm启动选项, 如-Xms1024M -Xmx1024M | `""` |
| `common.springProfile` | SpringBoot active profile | `dev` |
| `common.mountPath` | pod volume挂载路径 | `/data/storage` |

### 数据初始化job配置

|参数|描述|默认值 |
|---|---|---|
| `init.mongodb.enabled` | 是否初始化mongodb数据，支持幂等执行 | `true` |
| `init.mongodb.image` | mongodb job镜像拉取相关配置 | Check `values.yaml` |


### 网关配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `gateway.service.type` | 服务类型 | `ClusterIP` |
| `gateway.service.port` | 服务类型为`ClusterIP`时端口设置 | `80` |
| `gateway.service.nodePort` | 服务类型为`NodePort`时端口设置 | `80` |
| `gateway.dnsServer` | dns服务器地址，用于配置nginx resolver | `local=on`(openrestry语法，取本机`/etc/resolv.conf`配置) |
| `gateway.authorization` | 网关访问微服务认证信息 | `"Platform MThiNjFjOWMtOTAxYi00ZWEzLTg5YzMtMWY3NGJlOTQ0YjY2OlVzOFpHRFhQcWs4NmN3TXVrWUFCUXFDWkxBa00zSw=="` |
| `gateway.deployMode` | 部署模式，standalone: 独立模式，ci: 与ci搭配模式 | `standalone` |


### 公共配置项
***配置是需要加入前缀config.  如："config.BK_CODECC_CONSUL_DISCOVERY_TAG"
|参数|描述|默认值 |
|---|---|---|
|`BK_CODECC_CONSUL_DISCOVERY_TAG`|服务发现时的标签|codecc|
|`bkCiPublicUrl`|CI的公开地址|devops.example.com|
|`BK_CI_PRIVATE_URL`|CI的集群内地址|devops.example.com|
|`BK_CODECC_PUBLIC_URL`|codecc为集群外访问提供的URL|codecc.example.com|
|`BK_CODECC_PRIVATE_URL`|codecc为集群内访问提供的URL|codecc.example.com|
|`BK_CI_AUTH_PROVIDER`|服务权限校验方式|sample|
|`BK_IAM_PRIVATE_URL`||""|
|`BK_CODECC_APP_CODE`|CodeCC在蓝鲸体系中的唯一ID|bk_codecc|
|`BK_CODECC_APP_TOKEN`||""|
|`BK_PAAS_PRIVATE_URL`||pass.example.com|
|`BK_CI_IAM_CALLBACK_USER`||""|
|`BK_CODECC_PIPELINE_IMAGE_NAME`||bkci/ci|
|`BK_CODECC_PIPELINE_BUILD_TYPE`||""|
|`BK_CODECC_PIPELINE_IMAGE_TAG`||latest|
|`BK_CODECC_TASK_ENCRYPTOR_KEY`||yOB62XpuhiyWM|
|`BK_CODECC_PIPELINE_ATOM_CODE`|蓝盾流水线中CodeCC插件的AtomCode|CodeCCCheckAtom|
|`BK_CODECC_PIPELINE_ATOM_VERSION`|蓝盾流水线中CodeCC插件的版本|1.*|
|`BK_CODECC_PIPELINE_IMAGE_TYPE`||THIRD|
|`BK_CODECC_PIPELINE_SCM_IS_OLD_SVN`||true|
|`BK_CODECC_PIPELINE_SCM_IS_OLD_GITHUB`||true|
|`BK_CODECC_TASK_ANALYSIS_MAX_HOUR`|最大的分析时长|7|




### task服务配置

|参数|描述|默认值 |
|---|---|---|
| `task.enabled`       | 是否部署task     | `true`


### defect服务配置

|参数|描述|默认值 |
|---|---|---|
| `defect.enabled`       | 是否部署defect     | `true`         


### asyncreport服务配置

|参数|描述|默认值 |
|---|---|---|
| `asyncreport.enabled`       | 是否部署asyncreport     | `true`         

### report服务配置

|参数|描述|默认值 |
|---|---|---|
| `report.enabled`       | 是否部署report     | `true`                          |


###  使用默认的value.yaml文件部署即可部署