# BK-REPO

此Chart用于在Kubernetes集群中通过helm部署bktps

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 安装Chart
使用以下命令安装名称为`bktbs`的release, 其中`<bktbs helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <bktbs helm repo url>
$ helm install bktbs bkee/bktbs
```

上述命令将使用默认配置在Kubernetes集群中部署bktbs, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`bktbs`:

```shell
$ helm uninstall bktbs
```

上述命令将移除所有和bktbs相关的Kubernetes组件，并删除release。

## Chart依赖
- [bitnami/common](https://github.com/bitnami/charts/tree/master/bitnami/common)
- [bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/nginx-ingress-controller)
- [bitnami/miriadb](https://github.com/bitnami/charts/blob/master/bitnami/mariadb)
- [bitnami/etcd](https://github.com/bitnami/charts/blob/master/bitnami/etcd)

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

- server
- gateway
- dashboard

|参数|描述|默认值 |
|---|---|---|
| `image.registry`    | 镜像仓库              | `mirrors.tencent.com` |
| `image.repository`  | 镜像名称              | `bktbs/xxx`          |
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

### mariadb 配置
默认将部署mariadb，如果不需要可以关闭。
相关配置请参考[bitnami/mariadb](https://github.com/bitnami/charts/blob/master/bitnami/mariadb)

|参数|描述|默认值 |
|---|---|---|
| `mariadb.enabled` | 是否部署mariadb。如果需要使用外部数据库，设置为`false`并配置`externalDatabase` | `true` |
| `mariadb.auth.database` | mariadb数据库名称 | `tbs` |
| `mariadb.auth.username` | mariadb认证用户名 | `tbs` |
| `mariadb.auth.password` | mariadb密码 | `tbs-password` |
| `externalMongodb.host` | 外部mariadb服务的连接地址。当`mariadb.enabled`配置为`false`时，bktbs将使用此参数连接外部mariadb | `localhost` |
| `externalMongodb.port` | 外部mariadb数据库端口 | `3306` |
| `externalMongodb.user` | 外部mariadb认证用户名 | `` |
| `externalMongodb.password` | 外部mariadb密码 | `` |
| `externalMongodb.database` | 外部mariadb数据库 | `` |

### server配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `server.service.type` | 服务类型 | `ClusterIP` |
| `server.service.port` | 服务类型为`ClusterIP`时端口设置 | `30111` |
| `server.service.nodePort` | 服务类型为`NodePort`时端口设置 | `30111` |
| `server.host` | bktbs 地址 | `bktbs.com` |
| `server.etcdRootPath` | 在etcd中的根目录 | `/tbs_server` |
| `server.disttask_queue_list` | 加速队列配置 | `[K8S://default, K8S_WIN://default]` |
| `server.log.verbose` | 日志界别, 越大越详细 | `3` |
| `server.log.alsoLogToStdErr` | 日志打印到stderr | `true` |
| `server.bcs.clusterID` | 依赖的bcs集群id | `` |
| `server.bcs.apiToken` | 依赖的bcs访问api-token | `` |
| `server.bcs.apiAddress` | 依赖的bcs的api地址 | `https://127.0.0.1:8443` |
| `server.bcs.cpuPerInstance` | 依赖的bcs集群的单个加速实例cpu | `8` |
| `server.bcs.memPerInstance` | 依赖的bcs集群的单个加速实例mem | `16384` |
| `server.bcs.groupLabelKey` | 依赖的bcs集群node分组标签名 | `bk-turbo-az` |
| `server.bcs.platformLabelKey` | 依赖的bcs集群node系统标签名 | `kubernetes.io/os` |
| `server.bcs.disableWinHostNetwork` | 依赖的bcs集群是否禁用host network | `true` |

### gateway服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `gateway.service.type` | 服务类型 | `ClusterIP` |
| `gateway.service.port` | 服务类型为`ClusterIP`时端口设置 | `30113` |
| `gateway.service.nodePort` | 服务类型为`NodePort`时端口设置 | `30113` |
| `gateway.etcdRootPath` | 在etcd中的根目录 | `/tbs_gateway` |
| `gateway.log.verbose` | 日志界别, 越大越详细 | `3` |
| `gateway.log.alsoLogToStdErr` | 日志打印到stderr | `true` |

### dashboard服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `dashboard.service.type` | 服务类型 | `ClusterIP` |
| `dashboard.service.port` | 服务类型为`ClusterIP`时端口设置 | `30114` |
| `dashboard.service.nodePort` | 服务类型为`NodePort`时端口设置 | `30114` |
| `dashboard.log.verbose` | 日志界别, 越大越详细 | `3` |
| `dashboard.log.alsoLogToStdErr` | 日志打印到stderr | `true` |


您可以通过`--set key=value[,key=value]`来指定参数进行安装。例如，

```shell
$ helm install bktbs bkee/bktbs \
  --set global.imageRegistry=your.registry.com \
  --set gateway.host=your.bktbs.com
```


另外，也可以通过指定`YAML`文件的方式来提供参数，

```shell
$ helm install bktbs bkee/bktbs -f values
```

可以使用`helm show values`来获取默认配置，

```shell
# 查看默认配置
$ helm show values bkee/bktbs

# 保存默认配置到文件values.yaml
$ helm show values bkee/bktbs > values.yaml
```

## 配置案例

### 1. 使用已有的mariadb
```
# 关闭mariadb部署
mariadb.enabled=false
# 设置已有的mariadb
externalDatabase.host
externalDatabase.port
externalDatabase.username
externalDatabase.password
externalDatabase.database
```

### 2. 使用已有的ingress-controller
```
# 关闭nginx-ingress-controller部署
nginx-ingress-controller.enabled=false

# 根据需要配置ingress annotations
# ingress.annotations.key=value
```

### 5. 内网环境下，使用代理镜像仓库

- 单独修改
```
# 修改mariadb镜像仓库
mariadb.image.registry=xxx
# 修改nginx-ingress-controller镜像仓库
nginx-ingress-controller.image.registry=xxx
# 修改bktbs镜像仓库，xxx代表服务名称
xxx.image.registry=xxx
```

- 全局修改，应用到所有Charts
```
global.imageRegistry=xxx
```

## 常见问题

**1. 首次启动失败，bktbs Chart有问题吗**

答: bktbs的Chart依赖了`mariadb`,`etcd`和`nignx-ingress-controller`, 这三个依赖的Chart默认从docker.io拉镜像，如果网络不通或被docker hub限制，将导致镜像拉取失败，可以参考配置列表修改镜像地址。

**2. 首次启动时间过长，且READY状态为`0/1`？**

答: 如果选择了部署`mariadb Chart`，需要等待`mariadb`Ready后，`bktbs`相关组件才会Ready，这个期间容器状态为`Not Ready`。

**3. 如何查看日志？**

答: 有两种方式可以查看日志: 1. kubectl logs pod 查看实时日志  2.日志保存在/data/workspace/logs目录下，可以进入容器内查看
