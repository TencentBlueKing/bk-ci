# Chart 规范

[TOC]

## 0. 规则

- 保持统一
- 保持扁平/简单/直观. 避免中间层(用于生成chart的封装脚本或模板的模板等). 可以依赖[bitnami/common](https://github.com/bitnami/charts/tree/master/bitnami/common), 本项目自身一些封装放入`templates/_helpers.txt`.
- 一般一个项目一个目录. 
    - 如果运维及部署时有独立部署的需求, 一个项目可以拆分成多个目录(例如ci的各个模块需要在不同阶段部署). 多个目录需要保持统一的前缀及命名规则.
    - 否则, 项目内部模块需要通过子chart方式内部消化, 不要暴露到上层. 例如 `ci/charts/[app, cc]`

## 1 通用规范

### 1.1 Chart 基本配置

Chart.yaml 文件内必须声明以下字段：

- apiVersion: chart API 版本
- name: chart 名称
- version: 语义化版本 (每次交付新helm chart必须变更)
- description: 一句话对这个项目的描述(建议直接填写项目开源的英文介绍信息)
- appVersion: 包含的应用版本(一般等于版本tag)

注意:
- [强制要求] 每次变更交付helm charts, version必须更新
- [要求] appVersion同项目交包git tag版本一致

非必须依赖必须提供 `condition`，允许不启用，如：

```yaml
dependencies:
  - name: optional-chart
    condition: "optional-chart.enabled"
```

新增加的依赖必须执行以下命令以锁定依赖：

```shell
helm dependency build
```

另外，在 chart 中提供 README.md，编写的读者对象是部署该charts用户，需要描述清楚，依赖版本，前置准备条件，安装步骤，以及values.yaml重要配置项（或者全部配置项）的含义，配置方法。Readme还需要提供完整的卸载方法，包括本charts安装的依赖存储服务，以及helm hooks中定义的各类资源。
更多规范请见[官方规范](https://helm.sh/zh/docs/topics/charts/)。

示例:

```yaml
apiVersion: v2
name: bk-ci
description: A Helm chart for BlueKing CI
type: application
version: 1.16.1-beta2
appVersion: "1.16.0"
home: "https://github.com/Tencent/bk-ci"

dependencies:
- name: common
  version: 1.4.1
  repository: https://charts.bitnami.com/bitnami
- name: mysql
  version: 8.8.3
  repository: https://charts.bitnami.com/bitnami
  condition: mysql.enabled
```

### 1.2 Values

主要规范遵循 Helm 官方文档：[chart_best_practices/values](https://helm.sh/zh/docs/chart_best_practices/values/)。

另外：

- 重要: **变量名称以小写字母开头，单词按驼峰区分**；\
    - 示例: `bkLogConfig / bkDomain`
    - 专有名字缩写需统一转小写: `httpUrl / testHttpUrl / bkIam / bkCcUrl`
    - 不允许类似常量的命名存在`CUSTOM_URL / BK_DOMAIN`
- 大多数场景中，扁平的优于嵌套的，但当有大量的相关变量时，嵌套的值可以改善可读性；
- 给所有字符串打引号
    - 基于约定共识的key, 例如`registry/repository`等等, 不强制加(官方/第三方等存在大量没加的)
    - 自定义的, 即配置是项目特有的, 强制加(以便使用者识别类型是字符串, 不会混淆bool/int等);
- 使用 map 构建 values，方便用户使用 `--set`；
- 每个定义的属性都需要以对应名称开头，给出一句话描述对应的文档；
- 若 Charts 有不兼容之前部署方案的大改动，需要升级 Chart Version 大版本（如 0.1.1 -> 1.0.1）

```yaml
# 空值
空结构 storageSpec: {}
空列表 volumes: []
空字符串 nameOverride: ""


# 注释
# - 双#表示注释, 单#表示用户可以随时解开生效的配置项
# - 存在一行`##`进行分隔
# - 如果是通用的官方配置/第三方库配置, 尽量有ref, 链接指向相关文档

## If your etcd is not deployed as a pod, specify IPs it can be found on
##
endpoints: []
# - 10.1.1.1
# - 10.1.1.2

## Enable persistence using Persistent Volume Claims
## ref: https://kubernetes.io/docs/user-guide/persistent-volumes/
##
persistence:
  ## @param persistence.enabled Enable persistence using a `PersistentVolumeClaim`
  ##
  enabled: true
```

#### 1.2.1 参考 bitnami

类似需求的资源配置，考虑参考 bitnami 的 values：

```yaml
global:
  # 镜像拉取凭证，格式：
  # imagePullSecrets:
  #   - name: myRegistryKeySecretName
  imagePullSecrets: []
  # 源地址，优先级最高
  imageRegistry: ""
  # 持久卷存储类型，优先级最高
  storageClass: ""

# 副本数
replicaCount: 1

# k8s 版本
kubeVersion: ""

image:
  # 拉取策略
  pullPolicy: IfNotPresent
  # 镜像拉取凭证
  pullSecrets: []
  # 镜像源
  registry: docker.io
  # 镜像名
  repository: bitnami/xxxx
  # 镜像标签
  tag: 1.0.0

# 生成资源名称时替换 Chart 名称进行拼接
nameOverride: ""
# 生成资源名称时直接使用的名称（不进行拼接）
fullnameOverride: ""

serviceAccount:
  # 是否创建 service account
  create: true
  # 对应的注解
  annotations: {}
  # 对应的名称，如果没有设置，则自动生成
  name: ""

# pod 安全性上下文
podSecurityContext:
  enabled: true
  fsGroup: 1001

# container 安全性上下文
containerSecurityContext:
  enabled: true
  runAsUser: 1001

service:
  annotations: {}
  # 服务类型
  type: ClusterIP
  # 类型为 NodePort 时指定的端口
  nodePorts:
    http: 8080
    https: 8443
  # 对应端口
  port: 80
  httpsPort: 443

ingress:
  # 是否开启 ingress
  enabled: false
  annotations: {}
  # 使用的 api 版本，不自动获取
  apiVersion: ""
  # 域名
  hostname: example.local
  # 转发路径
  path: /
  # 转发方式
  pathType: ImplementationSpecific
  # 证书
  secrets: []
  # 是否开启证书
  tls: false
  # ingress class 名称
  ingressClassName: ""
  # 额外配置
  extraHosts: []
  extraPaths: []
  extraTls: []

# 资源限制
resources:
  limits: {}
  requests: {}

# 亲和性调度相关
nodeSelector: {}
tolerations: []
affinity: {}
podAffinityPreset: ""
podAntiAffinityPreset: soft
nodeAffinityPreset:
  key: ""
  type: ""
  values: []

# 更新策略
updateStrategy:
  type: RollingUpdate

# 覆盖的命令和参数
command: []
args: []

# 集群内访问的地址
clusterDomain: cluster.local

# 通用注解
commonAnnotations: {}
# 通用标签
commonLabels: {}
# pod 特殊设置的注解
podAnnotations: {}
# pod 特殊设置的标签
podLabels: {}

# 容器端口
containerPorts:
  http: 8080
  https: 8443

# 生命周期回调
lifecycleHooks: {}

# 自定义探针
customReadinessProbe: {}
customLivenessProbe: {}

# 存活探针配置
livenessProbe:
  enabled: true
  failureThreshold: 6
  initialDelaySeconds: 120
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5

# 就绪探针配置
readinessProbe:
  enabled: true
  failureThreshold: 6
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5

# 外部数据库
externalDatabase:
  host: ""
  password: ""
  port: 3306
  user: test

# Hosts 配置
hostAliases: []

# 持久化配置
persistence:
  accessModes: []
  enabled: true
  existingClaim: ""
  size: 8Gi
  storageClass: ""

# 额外资源定义
extraDeploy: []
extraEnvVars: []
extraEnvVarsCM: ""
extraEnvVarsSecret: ""
extraVolumeMounts: []
extraVolumes: []
initContainers: []
sidecars: []
```

注意: 
- 必须设置资源限制`resources.limits/resources.requests`; 根据经验值设置, 注意`resources.requests`不宜过大, 可能导致无法被调度;


#### 1.2.2 镜像配置
镜像地址分为3个值进行配置：

- `registry`：镜像源域名
- `repository`：镜像仓库，格式为：命名空间/镜像名称
- `tag`：镜像标签，使用语义化版本，如：1.2.3

实际使用的镜像地址为：`{{ registry }}/{{ repository }}:{{ tag }}`。

完整示例如下：

```yaml
image:
  # pullPolicy 为拉取策略
  pullPolicy: IfNotPresent

  # pullSecrets 为镜像拉取凭证，格式：
  # imagePullSecrets:
  #   - name: myRegistryKeySecretName
  pullSecrets: []

  # registry 为镜像源
  registry: docker.io

  # repository 为镜像名
  repository: blueking/xxxx

  # tag 为镜像标签
  tag: 1.0.0
```

#### 1.2.3 存储配置

存储配置可分为两种，内建的和外部的，其中内建使用子 chart 形式集成 [bitnami 提供的 charts](https://github.com/bitnami/charts/tree/master/bitnami)。

其中内建的存储配置由子 chart 定义，位于对应的 chart 名称对应的键之下。

其中 `host`，`password`，`port` 这几个配置参考 bitnami，其他存储配置类似。

##### 1.2.3.1 内建 Redis 配置参考

```yaml
redis:
  enabled: true

  nameOverride: "redis"
  architecture: standalone

  auth:
    password: blueking

  master:
    persistence:
      enabled: false

  replica:
    replicaCount: 1
    persistence:
      enabled: false

  image:
    registry: docker.io
    repository: bitnami/redis
    tag: 6.2.5-debian-10-r11
```

##### 1.2.3.2 内建 MariaDB 配置参考

```yaml
mariadb:
  enabled: true
  commonAnnotations: {}

  nameOverride: "mariadb"
  architecture: standalone

  auth:
    username: "admin"
    password: "blueking"
    rootPassword: "blueking"

  primary:
    service:
      port: 3306
    persistence:
      enabled: false

  image:
    registry: docker.io
    repository: bitnami/mariadb
    tag: 10.5.11-debian-10-r34

  initdbScriptsConfigMap: "init-sql"
```

##### 1.2.3.3 内建 RabbitMQ 配置参考

```yaml
rabbitmq:
  enabled: true

  nameOverride: "rabbitmq"

  auth:
    username: bk-apigateway
    password: blueking
    erlangCookie: blueking

  persistence:
    enabled: false

  image:
    registry: docker.io
    repository: bitnami/rabbitmq
    tag: 3.8.19-debian-10-r0
```

#### 1.2.4 Ingress 配置

简化的 ingress 配置如下：

```yaml
ingress:
  # 是否开启 ingress
  enabled: false
  # 域名
  hostname: example.local
  # 转发路径
  path: /
  # ingress class 名称
  ingressClassName: ""
```

### 1.3 NOTES.txt

为了方便用户使用，每个 Chart 都应该维护一个 NOTES.txt，存放简要的描述，提示用户helm install的后续操作步骤，包括但不限于用户访问产品的地址、用户名、密码等。如果是后台服务，需要输出对应的api地址、健康检查方式、metrics地址（如果有）。涉及到敏感信息，可以输出kubectl命令行语句提示用户到集群中自行获取。

NOTES.txt 使用中文编写，包含以下内容：

1. 感谢安装，展示 Chart 和 Release 名称；
2. 如何访问，如果有多个模块，请分别展示；
3. 如何验证系统已正确安装，给出指令（访问 healthz，获取状态）；
4. 如果有关键性 token 之类的配置生成，给出获取命令；
5. 如果使用了钩子，给出完整卸载的命令；
6. 输出命令的话，如果涉及namespace，都带上namespace的模板变量

## 2 扩展配置

### 2.1 ServiceMonitor

serviceMonitor用于采集pod暴露出的metrics指标

values.yaml

```yaml
bkmonitorConfig:
  enabled: false
```

templates/servicemonitor.yaml

```yaml
{{- if .Values.bkmonitorConfig.enabled }}
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: {{ include "common.names.fullname" . }}-bkmonitor
spec:
  endpoints:
    - interval: 30s # 采集周期
      path: /management/prometheus # 指标接口路径
      port: http # service的端口名，必须使用端口名，不能使用数字
  namespaceSelector:
    any: true
  selector: # 过滤出需要采集的service
    matchLabels:
      {{- if .Values.commonLabels }}
        {{- include "common.tplvalues.render" (dict "value" .Values.commonLabels "context" $) | nindent 6 }}
      {{- end }}
      app.kubernetes.io/component: microservice
      app.kubernetes.io/instance: {{ .Release.Name }}
      app.kubernetes.io/managed-by: Helm
{{- end }}
```

### 2.2 HPA

待补充

### 2.3 BkLogConfig

BkLogConfig是蓝鲸日志平台用于采集日志的CRD

values.yaml

```yaml
bklogConfig:
  enabled: false
  service:
    dataId: 1
  gatewayAccess:
    dataId: 1
  gatewayError:
    dataId: 1
```

templates/bklog.yaml

```yaml
{{- if .Values.bklogConfig.enabled }}
# Service Log
apiVersion: bk.tencent.com/v1alpha1
kind: BkLogConfig
metadata:
  name: {{ include "common.names.fullname" . }}-bklog-service
spec:
  dataId: {{ .Values.bklogConfig.service.dataId }}
  logConfigType: container_log_config
  namespace: {{ .Release.Namespace }}
  labelSelector:
      {{- if .Values.commonLabels }}
        {{- include "common.tplvalues.render" (dict "value" .Values.commonLabels "context" $) | nindent 6 }}
      {{- end }}
      app.kubernetes.io/component: microservice
      app.kubernetes.io/instance: {{ .Release.Name }}
      app.kubernetes.io/managed-by: Helm
  path:
    - /data/logs/*-.log
  encoding: 'utf-8'
  multiline:
    pattern: '^[0-2][0-9][0-9][0-9].[0-1][0-9].[0-3][0-9]'
    maxLines: 200
    timeout: '2s'
  ext_meta:
    logSourceType: "file"
{{- end }}
```

注意:
- 默认`bkLogConfig.enabled: false`的原因是, 在非蓝鲸环境测试时, 不会因为 CRD 不存在而导致部署失败; 
- `bkLogConfig.enabled: false`, 建议程序配置中将所有日志输出到stdout. `bkLogConfig.enabled: true`时, 正常输出到各个文件
- 未来日志采集会支持字段解析相关的配置(`清洗规则`), 所以建议所有日志输出统一使用`json logger`

## 3 最佳实践

待补充
