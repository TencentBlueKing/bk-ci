# BK-REPO

此Chart用于在Kubernetes集群中通过helm部署bkrepo

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 安装Chart
使用以下命令安装名称为`bkrepo`的release, 其中`<bkrepo helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <bkrepo helm repo url>
$ helm install bkrepo bkee/bkrepo
```

上述命令将使用默认配置在Kubernetes集群中部署bkrepo, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`bkrepo`:

```shell
$ helm uninstall bkrepo
```

上述命令将移除所有和bkrepo相关的Kubernetes组件，并删除release。

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
- repository
- auth
- generic
- docker
- npm
- pypi
- helm

|参数|描述|默认值 |
|---|---|---|
| `image.registry`    | 镜像仓库              | `mirrors.tencent.com` |
| `image.repository`  | 镜像名称              | `bkrepo/xxx`          |
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
| `podAntiAffinityPreset`              | Pod anti-affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                | `""`                         |
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
| `mongodb.auth.enabled` | 是否开启认证 | `true` |
| `mongodb.auth.database` | mongodb数据库名称 | `bkrepo` |
| `mongodb.auth.username` | mongodb认证用户名 | `bkrepo` |
| `mongodb.auth.password` | mongodb密码 | `bkrepo` |
| `externalMongodb.uri` | 外部mongodb服务的连接地址。当`mongodb.enabled`配置为`false`时，bkrepo将使用此参数连接外部mongodb | `mongodb://bkrepo:bkrepo@localhost:27017/bkrepo` |

> 如果需要持久化mongodb数据，请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)配置存储卷

### 数据持久化配置

数据持久化配置, 当使用filesystem方式存储时需要配置。

|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled` | 是否开启数据持久化，false则使用emptyDir类型volume, pod结束后数据将被清空，无法持久化 | `true` |
| `persistence.accessMode` | PVC Access Mode for bkrepo data volume | `ReadWriteOnce` |
| `persistence.size` | PVC Storage Request for bkrepo data volume | `100Gi` |
| `persistence.storageClass` | 指定storageClass。如果设置为"-", 则禁用动态卷供应; 如果不设置, 将使用默认的storageClass(minikube上是standard) | `nil` |
| `persistence.existingClaim` | 如果开启持久化并且定义了该项，则绑定k8s集群中已存在的pvc | `nil` |

> 如果开启数据持久化，并且没有配置`existingClaim`，将使用[动态卷供应](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)提供存储，使用`storageClass`定义的存储类。**在删除该声明后，这个卷也会被销毁(用于单节点环境，生产环境不推荐)。**。

### bkrepo公共配置

|参数|描述|默认值 |
|---|---|---|
| `common.imageRegistry` | bkrepo镜像仓库全局配置, 具有最高优先级 | `""` |
| `common.imageTag` | bkrepo镜像tag全局配置, 具有最高优先级 | `""` |
| `common.region` | 部署区域, 可不填 | `""` |
| `common.jvmOption` | jvm启动选项, 如-Xms1024M -Xmx1024M | `""` |
| `common.springProfile` | SpringBoot active profile | `dev` |
| `common.username` | bkrepo初始用户名 | `admin` |
| `common.password` | bkrepo初始密码 | `blueking` |
| `common.mountPath` | pod volume挂载路径 | `/data/storage` |
| `common.config.storage.type` | 存储类型，支持filesystem/cos/s3/hdfs | `filesystem` |
| `common.config.storage.filesystem.path` | filesystem存储方式配置，存储路径 | `/data/storage` |
| `common.config.storage.cos` | cos存储方式配置 | `nil` |
| `common.config.storage.s3` | s3存储方式配置 | `nil` |
| `common.config.storage.hdfs` | hdfs存储方式配置 | `nil` |

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
| `gateway.host` | bkrepo 地址 | `bkrepo.example.com` |
| `gateway.dnsServer` | dns服务器地址，用于配置nginx resolver | `local=on`(openrestry语法，取本机`/etc/resolv.conf`配置) |
| `gateway.authorization` | 网关访问微服务认证信息 | `"Platform MThiNjFjOWMtOTAxYi00ZWEzLTg5YzMtMWY3NGJlOTQ0YjY2OlVzOFpHRFhQcWs4NmN3TXVrWUFCUXFDWkxBa00zSw=="` |
| `gateway.deployMode` | 部署模式，standalone: 独立模式，ci: 与ci搭配模式 | `standalone` |

### repository服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `repository.config.deletedNodeReserveDays` | 节点被删除后多久清理数据 | `15` |

### auth服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `auth.config.realm` | 认证realm类型，支持local/devops | `local` |

### generic服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `generic.enabled`       | 是否部署generic     | `true`                      |
| `generic.config.domain` | generic domain地址 | `${gateway.host}/generic`   |

### docker registry服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `docker.enabled` | 是否部署docker | `false` |
| `docker.config ` | docker配置 | `{}` |

### npm registry服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `npm.enabled` | 是否部署npm | `false` |
| `npm.config.domain` | npm domain地址 | `${gateway.host}/npm`   |

### pypi registry服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `pypi.enabled` | 是否部署pypi | `false` |
| `pypi.config.domain` | pypi domain地址 | `${gateway.host}/pypi`  |

### helm registry服务配置

**以下为除Kubernetes组件通用配置之外的配置列表**

|参数|描述|默认值 |
|---|---|---|
| `helm.enabled` | 是否部署helm | `false` |
| `helm.config`  | helm配置 | `{}` |


您可以通过`--set key=value[,key=value]`来指定参数进行安装。例如，

```shell
$ helm install bkrepo bkee/bkrepo \
  --set global.imageRegistry=your.registry.com \
  --set gateway.host=bkrepo.example.com
```


另外，也可以通过指定`YAML`文件的方式来提供参数，

```shell
$ helm install bkrepo bkee/bkrepo -f values
```

可以使用`helm show values`来获取默认配置，

```shell
# 查看默认配置
$ helm show values bkee/bkrepo

# 保存默认配置到文件values.yaml
$ helm show values bkee/bkrepo > values.yaml
```

## 配置案例

### 1. 使用已有的mongodb
```
# 关闭mongodb部署
mongodb.enabled=false
# 设置已有的mongod连接字符串
common.mongodb.uri=mongodb://user:pass@mongodb-server:27017/bkrepo
```

### 2. 使用已有的ingress-controller
```
# 关闭nginx-ingress-controller部署
nginx-ingress-controller.enabled=false

# 根据需要配置ingress annotations
# ingress.annotations.key=value
```

### 3. 使用动态卷分配

通过使用storageClass动态绑定pv，假设我们创建了一个`storageClassName`为`standard`的pv，

```yaml
# pv.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: bkrepo-pv
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteOnce
  storageClassName: "standard" # minikube默认使用storageClass是standard
  hostPath:
    path: "/data/bkrepo"
```

```shell
$ kubectl create -f pv.yaml
```

因为在minikube默认使用storageClass是standard，所以使用默认配置进行部署，bkrepo以及mongodb的pvc将自动绑定到这个pv上。

同时，我们也可以指定自定义的`storageClass`，

```
# 开启数据持久化
persistence.enabled=true
persistence.storageClass=standard
persistence.accessModes=访问模式
persistence.size=pvc大小

# 如果有需要，同时配置mongodb持久化
mongodb.persistence.enabled=true
mongodb.persistence.storageClass=standard
```

也可以设置全局`storageClass`，将同时应用到bkrepo和mongodb中
```
global.storageClass=standard
```

### 4. 使用已有的pvc

```
# 开启数据持久化
persistence.enabled=true
persistence.existingClaim=your-persistent-volume-claim
persistence.accessModes=访问模式
persistence.size=pvc大小

# 如果有需要，同时配置mongodb持久化
mongodb.persistence.enabled=true
mongodb.persistence.existingClaim=your-persistent-volume-claim
```

### 5. 内网环境下，使用代理镜像仓库

- 单独修改
```
# 修改mongodb镜像仓库
mongodb.image.registry=xxx
# 修改nginx-ingress-controller镜像仓库
nginx-ingress-controller.image.registry=xxx
# 修改bkrepo镜像仓库，xxx代表服务名称
xxx.image.registry=xxx
```

- 全局修改，应用到所有Charts
```
global.imageRegistry=xxx
```

### 6. 配置不同的服务暴露方式

默认通过Ingress暴露服务，也可以使用以下方式：

- 使用NodePort直接访问

```
ingress.enabled=false
nginx-ingress-controller.enabled=false
gateway.service.type=NodePort
gateway.service.nodePort=30000
```
部署成功后，即可通过 bkrepo.example.com:\<nodePort\> 访问（您仍需要配置dns解析）

- 使用port-forward访问
```
ingress.enabled=false
nginx-ingress-controller.enabled=false
gateway.service.type=ClusterIP
```

部署成功后，通过`kubectl port-forward`将`bkrepo-gateway`服务暴露出去，即可通过 bkrepo.example.com:\<port\> 访问（您仍需要配置dns解析）
```shell
kubectl port-forward service/bkrepo-gateway <port>:80
```

## 常见问题

**1. 首次启动失败，是bkrepo Chart有问题吗**

答: bkrepo的Chart依赖了`mongodb`和`nignx-ingress-controller`, 这两个依赖的Chart默认从docker.io拉镜像，如果网络不通或被docker hub限制，将导致镜像拉取失败，可以参考配置列表修改镜像地址。

**2. 首次启动时间过长，且READY状态为`0/1`？**

答: 如果选择了部署`mongodb Chart`，需要等待`mongodb`部署完成后，`bkrepo`相关容器才会启动；启动过程涉及到数据表以及索引创建，这个期间容器状态为`Not Ready`。

**3. 我卸载了Release立即重新部署，Pod状态一直为`Pending`？**

答: 如果选择了默认方式使用动态卷供应，当使用`helm uninstall`卸载Release，随后创建的pvc也会被删除，如果在pvc被删除之前重新部署，新启动的`Pod`会进入`Pending`状态。

**4. 如何查看日志？**

答: 有两种方式可以查看日志: 1. kubectl logs pod 查看实时日志  2.日志保存在/data/workspace/logs目录下，可以进入容器内查看

**5. 为什么卸载之后重新安装，mongodb以及文件数据都没了？**

答: 默认创建的pv其`RECLAIM POLICY`为`DELETE`, 在删除该声明后，卷也会被销毁。如果需要数据持久化，请提前创建pvc好并设置`persistence`，