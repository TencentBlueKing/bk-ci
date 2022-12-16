# BK-CI

此Chart用于在Kubernetes集群中通过helm部署bkci

## 环境要求
- Kubernetes 1.16+
- Helm 3+

## 生成Chart
- 执行命令 : `./build_chart.py ${GATEWAY_DOCKER_IMAGE_VERSION} ${BACKEND_DOCKER_IMAGE_VERSION}` *(注:GATEWAY_DOCKER_IMAGE_VERSION , BACKEND_DOCKER_IMAGE_VERSION 为镜像的version , 具体查看bk-ci/docker-images/core/ci/README.md)*
- 可选项:
  - 在`./build/values.json`中配置默认项 (配置项参考[服务配置](#服务配置)) , 如:
    ```
    {
      "bkCiHost": "devops.example.com",
      "bkCiPublicUrl": "devops.example.com",
      "bkCiPublicHostIp": "127.0.0.1"
    }
    ```

## 安装Chart
使用以下命令安装名称为`bkci`的release:

```shell
$ helm install bkci .
```

上述命令将使用默认配置在Kubernetes集群中部署bkci, 并输出访问指引。

部署默认k8s构建机参考[kubernetes-manager部署文档.md](./kubernetes-manager部署文档.md)

## 卸载Chart
使用以下命令卸载`bkci`:

```shell
$ helm uninstall bkci
```

上述命令将移除所有和bkci相关的Kubernetes组件，并删除release。

*注: helm现在不会主动删除pvc, 所以在使用内置数据存储的时候, 要想彻底删除所有数据, 需要手动清理, 如: kubectl get pvc|awk '{print $1}'|grep -v 'NAME'|xargs kubectl delete pvc*

## Chart依赖
- [bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/nginx-ingress-controller)
- [bitnami/mysql](https://github.com/bitnami/charts/blob/master/bitnami/mysql)
- [bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)
- [bitnami/elasticsearch](https://github.com/bitnami/charts/blob/master/bitnami/elasticsearch)
- [bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)
- [bitnami/influxdb](https://github.com/bitnami/charts/blob/master/bitnami/influxdb)
- [bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

## RBAC配置

|参数|描述|默认值|
|---|---|---|
| `rbac.serviceAccount` | RBAC账户 | `bkci` |

## 镜像配置

能够配置的镜像有:
- gatewayImage
- backendImage

|参数|描述|默认值|
|---|---|---|
| `registry` | 镜像仓库 | `mirrors.tencent.com/bkce` |
| `repository` | 镜像名称 | `bkci/gateway` / `bkci/backend` |
| `tag` | 镜像tag | `1.16.0` |
| `pullPolicy` | 镜像拉取策略 | `IfNotPresent` |
| `pullSecrets` | 镜像拉取Secret名称数组 | `[]` |

## 蓝鲸日志采集配置
|参数|描述|默认值|
|---|---|---|
| `bkLogConfig.enabled` | 是否开启日志采集 | `false` |
| `bkLogConfig.service.dataId` | 服务日志采集ID | `1` |
| `bkLogConfig.gatewayAccess.dataId` | 网关访问日志采集ID | `1` |
| `bkLogConfig.gatewayError.dataId` | 网关异常日志采集ID | `1` |
| `bkLogConfig.turbo.enabled` | 是否开启turbo的日志采集 | `false` |
| `bkLogConfig.turbo.dataId` | turbo日志采集ID | `1` |

## 蓝鲸监控配置
|参数|描述|默认值|
|---|---|---|
| `serviceMonitor.enabled` | 是否开启蓝鲸监控 | `false` |
| `serviceMonitor.turbo.enabled` | turbo是否开启蓝鲸监控 | `false` |

## 初始化配置
|参数|描述|默认值|
|---|---|---|
| `init.sql.enabled` | 是否初始化数据库 | `true` |
| `init.iam.enabled` | 是否初始化权限中心 | `true` |
| `init.turbo.enabled` | 是否初始化编译加速 | `true` |
| `init.bkrepo.enabled` | 是否初始化制品库 | `true` |
| `init.defaultImage.enabled` | 是否初始化构建镜像 | `true` |

## ingress 配置

|参数|描述|默认值 |
|---|---|---|
| `ingress.enabled` | 是否创建ingress | `true` |
| `annotations` | ingress标注 | Check `values.yaml` |

默认不会部署`nginx-ingress-controller`
相关配置请参考[bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/)

|参数|描述|默认值 |
|---|---|---|
| `nginx-ingress-controller.enabled` | 是否部署nginx ingress controller | `false` |
| `nginx-ingress-controller.defaultBackend.enabled` | nginx ingress controller默认backend | `false` |

## 组件配置

能够配置的组件有:
- artifactory
- auth
- dispatch
- dispatch-docker
- environment
- gateway
- image
- log
- misc
- notify
- openapi
- plugin
- process
- project
- quality
- repository
- store
- ticket
- websocket

|参数|描述|默认值 |
|---|---|---|
| `replicas`                       | Number of pod 1  (only when `autoscaling.enabled=false`)                                                                    | `1`                                                     |
| `resources.limits`                   | The resources limits for containers                                                          | `{cpu:500m ,memory:1500Mi}`                                                    |
| `resources.requests`                 | The requested resources for containers                                                       | `{cpu:100m ,memory:1000Mi}`                                                    |
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

HPA设置
|参数|描述|默认值 |
|---|---|---|
| `autoscaling.enabled` | 是否开启hpa | `false` |
| `autoscaling.minReplicas` | 最小分片数 | `1` | 
| `autoscaling.maxReplicas` | 最大分片数 | `3` |
| `autoscaling.targetCPU` | CPU阈值 | `80` |
| `autoscaling.targetMemory` | 内存阈值 | `80` |

其中除了`gateway` , 其他组件还可以配置jvm的内存

|参数|描述|默认值 |
|---|---|---|
| `env.jvmXms` | JVM初始内存 | `512m` |
| `env.jvmXmx` | JVM最大内存(不能超过limit) | `1024m` | 

## mysql 配置
默认将部署 mysql ，如果不需要可以关闭。
相关配置请参考[bitnami/mysql](https://github.com/bitnami/charts/blob/master/bitnami/mysql)

|参数|描述|默认值 |
|---|---|---|
| `mysql.enabled` | 是否部署mysql。否则会使用externalMysql配置 | `true` |
| `mysql.rootPassword` | root密码 | `blueking` |
| `externalMysql.host` | 外部地址 | `localhost` |
| `externalMysql.port` | 外部端口 | `3306` |
| `externalMysql.username` | 外部用户名 | `bkci` |
| `externalMysql.password` | 外部密码 | `bkci` |

## redis 配置
默认将部署 redis , 如果不需要可以关闭。
相关配置请参考[bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)

|参数|描述|默认值 |
|---|---|---|
| `redis.enabled` | 是否部署redis。否则会使用externalRedis配置 | `true` |
| `redis.auth.password` | 密码 | `user` |
| `externalRedis.host` | 外部地址 | `localhost` |
| `externalRedis.port` | 外部端口 | `6379` |
| `externalRedis.password` | 外部密码 | `bkci` |

## elasticsearch 配置
默认将部署 elasticsearch , 如果不需要可以关闭。
相关配置请参考[bitnami/elasticsearch](https://github.com/bitnami/charts/blob/master/bitnami/elasticsearch)

|参数|描述|默认值 |
|---|---|---|
| `elasticsearch.enabled` | 是否部署elasticsearch。否则会使用externalElasticsearch配置 | `true` |
| `externalElasticsearch.host` | 外部地址 | `localhost` |
| `externalElasticsearch.port` | 外部端口 | `9200` |
| `externalElasticsearch.username` | 外部用户名 | `bkci` |
| `externalElasticsearch.password` | 外部密码 | `bkci` |

## rabbitmq 配置
默认将部署 rabbitmq , 如果不需要可以关闭。
相关配置请参考[bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)

|参数|描述|默认值 |
|---|---|---|
| `rabbitmq.enabled` | 是否部署rabbitmq。否则会使用externalRabbitmq配置 | `true` |
| `rabbitmq.auth.username` | 用户名 | `user` |
| `rabbitmq.auth.password` | 密码 | `user` |
| `externalRabbitmq.host` | 外部地址 | `localhost` |
| `externalRabbitmq.vhost` | 外部vhost | `bkci` |
| `externalRabbitmq.username` | 外部用户名 | `bkci` |
| `externalRabbitmq.password` | 外部密码 | `bkci` |

## influxdb 配置
默认将部署 influxdb , 如果不需要可以关闭。
相关配置请参考[bitnami/influxdb](https://github.com/bitnami/charts/blob/master/bitnami/influxdb)

|参数|描述|默认值 |
|---|---|---|
| `influxdb.enabled` | 是否部署influxdb。否则会使用externalInfluxdb配置 | `true` |
| `influxdb.auth.admin.username` | 用户名 | `user` |
| `influxdb.auth.admin.password` | 密码 | `password` |
| `externalInfluxdb.host` | 外部地址 | `localhost` |
| `externalInfluxdb.port` | 外部端口 | `8086` |
| `externalInfluxdb.username` | 外部用户名 | `bkci` |
| `externalInfluxdb.password` | 外部密码 | `bkci` |

## mongodb 配置
默认将部署 mongodb , 如果不需要可以关闭。
相关配置请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

|参数|描述|默认值 |
|---|---|---|
| `mongodb.enabled` | 是否部署mongodb。否则会使用externalMongodb配置 | `true` |
| `mongodb.auth.username` | 用户名 | `true` |
| `mongodb.auth.password` | 密码 | `true` |
| `externalMongodb.turbo.turboUrl` | 外部turboUrl | `mongodb://bkci:bkci@localhost:27017/db_turbo` |
| `externalMongodb.turbo.quartzUrl` | 外部quartzUrl | `mongodb://bkci:bkci@localhost:27017/db_quart` |

### 数据持久化配置

数据持久化配置, 当使用filesystem方式存储时需要配置。

|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled` | 是否开启数据持久化，false则使用emptyDir类型volume, pod结束后数据将被清空，无法持久化 | `true` |
| `persistence.accessMode` | PVC Access Mode for bkrepo data volume | `ReadWriteOnce` |
| `persistence.size` | PVC Storage Request for bkrepo data volume | `10Gi` |
| `persistence.storageClass` | 指定storageClass。如果设置为"-", 则禁用动态卷供应; 如果不设置, 将使用默认的storageClass(minikube上是standard) | `nil` |
| `persistence.existingClaim` | 如果开启持久化并且定义了该项，则绑定k8s集群中已存在的pvc | `nil` |
| `persistence.mountPath` | pv挂载的路径 | `/data1` |

> 如果开启数据持久化，并且没有配置`existingClaim`，将使用[动态卷供应](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)提供存储，使用`storageClass`定义的存储类。**在删除该声明后，这个卷也会被销毁(用于单节点环境，生产环境不推荐)。**。

<div id="服务配置"></div>

### 服务配置
*以下host如果使用k8s的service name , 请使用全限定名称 , 如 bkssm-web.default.svc.cluster.local*
*详情以build_chart.py生成的values.yaml为准*

|参数|描述|默认值 |
|---|---|---|
| `bkCiAppCode`  | 应用Code | `"bk_ci"` |
| `bkCiAppToken`  | 应用Token | `""` |
| `bkCiArtifactoryRealm`  | 仓库使用类型 | `local` |
| `bkCiAuthProvider`  | 鉴权方式 | `sample` |
| `bkCiBkrepoAuthorization`  | 制品库鉴权标识 | `""` |
| `bkCiDataDir`  | 数据目录 | `/data/dir/` |
| `bkCiDockerImagePrefix`  | Docker镜像前缀 | `""` |
| `bkCiDockerRegistryPassword`  | Docker仓库密码 | `""` |
| `bkCiDockerRegistryUrl`  | Docker仓库地址 | `""` |
| `bkCiDockerRegistryUser`  | Docker仓库用户 | `""` |
| `bkCiDockerUrl`  | Docker的web入口 | `""` |
| `bkCiDocsUrl`  | 文档地址 | `https://docs.bkci.net/` |
| `bkCiEnvironmentAgentCollectorOn`  | 第三方构建机状态上报 | `true` |
| `bkCiEsClusterName`  | ES的集群名 | `devops` |
| `bkCiFqdn`  | CI的其他域名,空格分隔 | `""` |
| `bkCiFqdnCert`  | BKCI站点的HTTPS证书存储位置 | `""` |
| `bkCiGatewayCorsAllowList`  | 网关允许cors的来源域名 | `""` |
| `bkCiGatewayDnsAddr`  | 网关使用的dns | `local=on` |
| `bkCiGatewayRegionName`  | 网关的区域 | `""` |
| `bkCiGatewaySsmTokenUrl`  | 网关用户认证token验证URL的路径 | `""` |
| `bkCiHome`  | CI根目录 | `/data/bkee/ci` |
| `bkCiHost`  | CI域名 | `devops.example.com` |
| `bkCiHttpsPort`  | CI使用https时的端口 | `80` |
| `bkCiHttpPort`  | CI使用http时的端口 | `80` |
| `bkCiIamCallbackUser`  | 供iam系统发起回调时使用的用户名 | `"bk_iam"` |
| `bkCiIamWebUrl`  | IAM SaaS入口url | `""` |
| `bkCiInfluxdbDb`  | influxdb数据库 | `"agentMetrix"` |
| `bkCiJfrogFqdn`  | jFrog完全合格域名 | `""` |
| `bkCiJfrogHttpPort`  | jFrog构件下载服务的http端口 | `80` |
| `bkCiJfrogPassword`  | jFrog构件下载服务的密码 | `""` |
| `bkCiJfrogUrl`  | jFrog构件下载服务的链接 | `""` |
| `bkCiJfrogUser`  | jFrog构件下载服务的用户 | `""` |
| `bkCiJobFqdn`  | job完全合格域名 | `""` |
| `bkCiJwtRsaPrivateKey`  | JWT RSA密钥对 | `""` |
| `bkCiJwtRsaPublicKey`  | JWT RSA密钥对 | `""` |
| `bkCiLogsDir`  | 日志存放地址 | `/data/logs` |
| `bkCiLogCloseDay`  | 定时清理构建日志--关闭索引 | `""` |
| `bkCiLogDeleteDay`  | 定时清理构建日志--删除索引 | `""` |
| `bkCiLogStorageType`  | 日志存储方式 lucene/elasticsearch | `elasticsearch` |
| `bkCiLuceneDataDir`  | log直接使用lucene时的数据目录 | `""` |
| `bkCiLuceneIndexMaxSize`  | log直接使用lucene时最大值 | `""` |
| `bkCiPaasDialogLoginUrl`  | 蓝鲸登录小窗 | `""` |
| `bkCiPaasLoginUrl`  | 跳转到蓝鲸登录服务主页 | `""` |
| `bkCiPrivateUrl`  | 蓝鲸集群内使用的url, 如iam回调ci时 | `""` |
| `bkCiProcessEventConcurrent`  | process并发保护 | `10` |
| `bkCiPublicUrl`  | CI的域名 | `devops.example.com` |
| `bkCiPublicHostIp` | 对外IP | `127.0.0.1` |
| `bkCiRedisDb`  | redis数据库 | `0` |
| `bkCiRedisSentinelAddr`  | redis哨兵地址 | `""` |
| `bkCiRedisSentinelMasterName`  | redis哨兵名称 | `""` |
| `bkCiRepositoryGithubApp`  | github配置 | `""` |
| `bkCiRepositoryGithubClientId`  | github配置 | `""` |
| `bkCiRepositoryGithubClientSecret`  | github配置 | `""` |
| `bkCiRepositoryGithubSignSecret`  | github配置 | `""` |
| `bkCiRepositoryGitlabUrl`  | gitlab配置 | `""` |
| `bkCiRepositoryGitPluginGroupName`  | git插件分组 | `""` |
| `bkCiRepositoryGitPrivateToken`  | git的token | `""` |
| `bkCiRepositoryGitUrl`  | git地址 | `""` |
| `bkCiRepositorySvnApiKey`  | svn的key | `""` |
| `bkCiRepositorySvnApiUrl`  | svn的地址 | `""` |
| `bkCiRepositorySvnWebhookUrl`  | svn的回调地址 | `""` |
| `bkCiS3AccessKey`  | s3的访问key | `""` |
| `bkCiS3BucketName`  | s3的名称 | `""` |
| `bkCiS3EndpointUrl`  | s3的端点 | `""` |
| `bkCiS3SecretKey`  | s3的秘钥 | `""` |
| `bkCiStoreUserAvatarsUrl`  | PaaS用户头像, 目前仅显示默认头像 | `""` |
| `bkDomain`  | 建议使用用户持有的公网域名(但解析为内网IP) | `""` |
| `bkHome`  | 蓝鲸根目录 | `""` |
| `bkHttpSchema`  | http协议 | `http` |
| `bkIamPrivateUrl`  | iam内部地址 | `""` |
| `bkLicensePrivateUrl`  | 协议内部地址 | `""` |
| `bkPaasFqdn`  | paas域名 | `""` |
| `bkPaasHttpsPort`  | paas端口 | `80` |
| `bkPaasPrivateUrl`  | paas内部地址 | `""` |
| `bkPaasPublicUrl`  | paas外部地址 | `""` |
| `bkRepoHost`  | 制品库地址 | `""` |
| `bkSsmHost`  | 用户认证地址 | `""` |
| `bkSsmPort`  | 用户认证端口 | `80` |
| `bkCiNotifyWeworkSendChannel` | 通知渠道 | `weworkAgent` |

### 编译加速配置
|参数|描述|默认值 |
|---|---|---|
| `turbo.enabled`  | 是否开启编译加速 | `"false"` |
| `turbo.config.tbs.rootpath`  | 编译加速的地址 | `""` |
| `turbo.config.tbs.urltemplate`  | 编译加速的调用url | `"api/v1/{engine}/resource/{resource_type}"` |
| `turbo.config.tbs.dashboard`  | 编译加速管理地址 | `""` |
| `turbo.config.devops.rootpath`  | 蓝盾url | `""` |

**以下为除Kubernetes组件通用配置之外的配置列表**

您可以通过`--set key=value[,key=value]`来指定参数进行安装。例如，

```shell
$ helm install bkci . \
  --set global.imageRegistry=your.registry.com \
  --set gateway.host=your.bkci.com
```


另外，也可以通过指定`YAML`文件的方式来提供参数，

```shell
$ helm install bkci . -f values
```

可以使用`helm show values`来获取默认配置，

```shell
# 查看默认配置
$ helm show values .
```
