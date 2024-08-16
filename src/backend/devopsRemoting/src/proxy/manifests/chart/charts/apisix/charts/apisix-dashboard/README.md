# Apache APISIX Dashboard

[APISIX Dashboard](https://github.com/apache/apisix-dashboard/) is designed to make it as easy as possible for users to operate Apache APISIX through a frontend interface.

This chart bootstraps an apisix-dashboard deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

APISIX Dashboard requires Kubernetes version 1.14+.

## Get Repo Info

```console
helm repo add apisix https://charts.apiseven.com
helm repo update
```

## Install Chart

**Important:** only helm3 is supported

```console
helm install [RELEASE_NAME] apisix/apisix-dashboard --namespace ingress-apisix --create-namespace
```

The command deploys apisix-dashboard on the Kubernetes cluster in the default configuration.

_See [configuration](#configuration) below._

_See [helm install](https://helm.sh/docs/helm/helm_install/) for command documentation._

## Uninstall Chart

```console
helm uninstall [RELEASE_NAME] --namespace ingress-apisix
```

This removes all the Kubernetes components associated with the chart and deletes the release.

_See [helm uninstall](https://helm.sh/docs/helm/helm_uninstall/) for command documentation._

## Upgrading Chart

```console
helm upgrade [RELEASE_NAME] [CHART] --install
```

_See [helm upgrade](https://helm.sh/docs/helm/helm_upgrade/) for command documentation._

## Parameters

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` |  |
| autoscaling.enabled | bool | `false` | Enable autoscaling for Apache APISIX Dashboard deployment |
| autoscaling.maxReplicas | int | `100` | Maximum number of replicas to scale out |
| autoscaling.minReplicas | int | `1` | Minimum number of replicas to scale back |
| autoscaling.targetCPUUtilizationPercentage | int | `80` | Target CPU utilization percentage |
| config.authentication.expireTime | int | `3600` | JWT token expire time, in second |
| config.authentication.secret | string | `"secret"` | Secret for jwt token generation |
| config.authentication.users | list | `[{"password":"admin","username":"admin"}]` | Specifies username and password for login manager api. |
| config.conf.etcd.endpoints | list | `["apisix-etcd:2379"]` | Supports defining multiple etcd host addresses for an etcd cluster |
| config.conf.etcd.password | string | `nil` | Specifies etcd basic auth password if enable etcd auth |
| config.conf.etcd.prefix | string | `"/apisix"` | apisix configurations prefix |
| config.conf.etcd.username | string | `nil` | Specifies etcd basic auth username if enable etcd auth |
| config.conf.listen.host | string | `"0.0.0.0"` | The address on which the Manager API should listen. The default value is 0.0.0.0, if want to specify, please enable it. This value accepts IPv4, IPv6, and hostname. |
| config.conf.listen.port | int | `9000` | The port on which the Manager API should listen. |
| config.conf.log.accessLog.filePath | string | `"/dev/stdout"` | Error log path |
| config.conf.log.errorLog | object | `{"filePath":"/dev/stderr","level":"warn"}` | Error log level. Supports levels, lower to higher: debug, info, warn, error, panic, fatal |
| config.conf.log.errorLog.filePath | string | `"/dev/stderr"` | Access log path |
| fullnameOverride | string | `""` | String to fully override apisix-dashboard.fullname template |
| image.pullPolicy | string | `"IfNotPresent"` | Apache APISIX Dashboard image pull policy |
| image.repository | string | `"apache/apisix-dashboard"` | Apache APISIX Dashboard image repository |
| image.tag | string | `"3.0.0-alpine"` |  |
| imagePullSecrets | list | `[]` | Docker registry secret names as an array |
| ingress.annotations | object | `{}` | Ingress annotations |
| ingress.className | string | `""` | Kubernetes 1.18+ support ingressClassName attribute |
| ingress.enabled | bool | `false` | Set to true to enable ingress record generation |
| ingress.hosts | list | `[{"host":"apisix-dashboard.local","paths":[]}]` | The list of hostnams to be covered with this ingress record |
| ingress.tls | list | `[]` | Create TLS Secret |
| labelsOverride | object | `{}` | Override default labels assigned to Apache APISIX dashboard resource |
| nameOverride | string | `""` | String to partially override apisix-dashboard.fullname template (will maintain the release name) |
| nodeSelector | object | `{}` | Node labels for pod assignment |
| podAnnotations | object | `{}` | Apache APISIX Dashboard Pod annotations |
| podSecurityContext | object | `{}` | Set the securityContext for Apache APISIX Dashboard pods |
| priorityClassName | string | `""` | Set the [priorityClassName](https://kubernetes.io/docs/concepts/scheduling-eviction/pod-priority-preemption/#pod-priority) for pods |
| replicaCount | int | `1` | Number of Apache APISIX Dashboard nodes |
| resources | object | `{}` |  |
| securityContext | object | `{}` | Set the securityContext for Apache APISIX Dashboard container |
| service.port | int | `80` | Service HTTP port |
| service.type | string | `"ClusterIP"` | Service type |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| tolerations | list | `[]` | Tolerations for pod assignment |
