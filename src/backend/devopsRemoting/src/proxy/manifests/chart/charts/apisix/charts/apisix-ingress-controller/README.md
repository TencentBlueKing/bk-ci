# Apache APISIX ingress controller

[APISIX Ingress controller](https://github.com/apache/apisix-ingress-controller/) for Kubernetes using Apache APISIX as a high performance reverse proxy and load balancer.

If you have installed multiple ingress controller, add the `kubernetes.io/ingress.class: apisix` annotation to your Ingress resources.

This chart bootstraps an apisix-ingress-controller deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

Apisix ingress controller requires Kubernetes version 1.16+.

## Get Repo Info

```console
helm repo add apisix https://charts.apiseven.com
helm repo update
```

## Install Chart

**Important:** only helm3 is supported

```console
helm install [RELEASE_NAME] apisix/apisix-ingress-controller --namespace ingress-apisix --create-namespace
```

The command deploys apisix-ingress-controller on the Kubernetes cluster in the default configuration.

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

## Configuration

See [Customizing the Chart Before Installing](https://helm.sh/docs/intro/using_helm/#customizing-the-chart-before-installing). To see all configurable options with detailed comments, visit the chart's [values.yaml](./values.yaml), or run these configuration commands:

```console
helm show values apisix/apisix-ingress-controller
```

### Pod priority

`priorityClassName` field referenced a name of a created `PriorityClass` object. Check [here](https://kubernetes.io/docs/concepts/scheduling-eviction/pod-priority-preemption) for more details.

### Security context

A security context provides us with a way to define privilege and access control for a Pod or even at the container level.

Check [here](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.23/#securitycontext-v1-core) to see the SecurityContext resource with more detail.

Check also [here](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/) to see a full explanation and some examples to configure the security context.

Right below you have an example of the security context configuration. In this case, we define that all the processes in the container will run with user ID 1000.

```yaml
...

spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 3000
...
```

The same for the group definition, where we define the primary group of 3000 for all processes.

**It's quite important to know, if the `runAsGroup` is omited, the primary group will be root(0)**, which in some cases goes against some security policies.

To define this configuration at the **pod level**, you need to set:

```yaml
    --set podSecurityContext.runAsUser=«VALUE»
    --set podSecurityContext.runAsGroup=«VALUE»
    ...
```

The same for container level, you need to set:

```yaml
    --set securityContext.runAsUser=«VALUE»
    --set SecurityContext.runAsGroup=«VALUE»
    ...
```

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| autoscaling.enabled | bool | `false` |  |
| autoscaling.maxReplicas | int | `100` |  |
| autoscaling.minReplicas | int | `1` |  |
| autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| clusterDomain | string | `"cluster.local"` |  |
| config.apisix | object | `{"adminAPIVersion":"v2","adminKey":"edd1c9f034335f136f87ad84b625c8f1","clusterName":"default","serviceName":"apisix-admin","serviceNamespace":"ingress-apisix","servicePort":9180}` | APISIX related configurations. |
| config.apisix.adminAPIVersion | string | `"v2"` | the APISIX admin API version. can be "v2" or "v3", default is "v2". |
| config.apisix.serviceName | string | `"apisix-admin"` | Enabling this value, overrides serviceName and serviceNamespace. serviceFullname: "apisix-admin.apisix.svc.local" |
| config.apisixResourceSyncInterval | string | `"1h"` | Default interval for synchronizing Kubernetes resources to APISIX |
| config.certFile | string | `"/etc/webhook/certs/cert.pem"` | the TLS certificate file path. |
| config.enableProfiling | bool | `true` | enable profiling via web interfaces host:port/debug/pprof, default is true. |
| config.httpListen | string | `":8080"` | the HTTP Server listen address, default is ":8080" |
| config.httpsListen | string | `":8443"` | the HTTPS Server listen address, default is ":8443" |
| config.ingressPublishService | string | `""` | the controller will use the Endpoint of this Service to update the status information of the Ingress resource. The format is "namespace/svc-name" to solve the situation that the data plane and the controller are not deployed in the same namespace. |
| config.ingressStatusAddress | list | `[]` |  |
| config.keyFile | string | `"/etc/webhook/certs/key.pem"` | the TLS key file path. |
| config.kubernetes | object | `{"apiVersion":"apisix.apache.org/v2","apisixRouteVersion":"apisix.apache.org/v2","electionId":"ingress-apisix-leader","enableGatewayAPI":false,"ingressClass":"apisix","ingressVersion":"networking/v1","kubeconfig":"","namespaceSelector":[""],"pluginMetadataCM":"","resyncInterval":"6h","watchEndpointSlices":false}` | Kubernetes related configurations. |
| config.kubernetes.apiVersion | string | `"apisix.apache.org/v2"` | the resource API version, support "apisix.apache.org/v2beta3" and "apisix.apache.org/v2". default is "apisix.apache.org/v2" |
| config.kubernetes.apisixRouteVersion | string | `"apisix.apache.org/v2"` | the supported apisixroute api group version, can be "apisix.apache.org/v2" "apisix.apache.org/v2beta3" or "apisix.apache.org/v2beta2" |
| config.kubernetes.electionId | string | `"ingress-apisix-leader"` | the election id for the controller leader campaign, only the leader will watch and delivery resource changes, other instances (as candidates) stand by. |
| config.kubernetes.enableGatewayAPI | bool | `false` | whether to enable support for Gateway API. Note: This feature is currently under development and may not work as expected. It is not recommended to use it in a production environment. Before we announce support for it to reach Beta level or GA. |
| config.kubernetes.ingressClass | string | `"apisix"` | The class of an Ingress object is set using the field IngressClassName in Kubernetes clusters version v1.18.0 or higher or the annotation "kubernetes.io/ingress.class" (deprecated). |
| config.kubernetes.ingressVersion | string | `"networking/v1"` | the supported ingress api group version, can be "networking/v1beta1", "networking/v1" (for Kubernetes version v1.19.0 or higher), and "extensions/v1beta1", default is "networking/v1". |
| config.kubernetes.kubeconfig | string | `""` | the Kubernetes configuration file path, default is "", so the in-cluster configuration will be used. |
| config.kubernetes.namespaceSelector | list | `[""]` | namespace_selector represent basis for selecting managed namespaces. the field is support since version 1.4.0 For example, "apisix.ingress=watching", so ingress will watching the namespaces which labels "apisix.ingress=watching" |
| config.kubernetes.pluginMetadataCM | string | `""` | Pluginmetadata in APISIX can be controlled through ConfigMap. default is "" |
| config.kubernetes.resyncInterval | string | `"6h"` | how long should apisix-ingress-controller re-synchronizes with Kubernetes, default is 6h, |
| config.kubernetes.watchEndpointSlices | bool | `false` | whether to watch EndpointSlices rather than Endpoints. |
| config.logLevel | string | `"info"` | the error log level, default is info, optional values are: debug, info, warn, error, panic, fatal |
| config.logOutput | string | `"stderr"` | the output file path of error log, default is stderr, when the file path is "stderr" or "stdout", logs are marshalled plainly, which is more readable for human; otherwise logs are marshalled in JSON format, which can be parsed by programs easily. |
| fullnameOverride | string | `""` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.repository | string | `"apache/apisix-ingress-controller"` |  |
| image.tag | string | `"1.6.0"` |  |
| imagePullSecrets | list | `[]` |  |
| initContainer.image | string | `"busybox"` |  |
| initContainer.tag | float | `1.28` |  |
| labelsOverride | object | `{}` | Override default labels assigned to Apache APISIX ingress controller resource |
| nameOverride | string | `""` | Default values for apisix-ingress-controller. This is a YAML-formatted file. Declare variables to be passed into your templates.  |
| nodeSelector | object | `{}` |  |
| podAnnotations | object | `{}` |  |
| podSecurityContext | object | `{}` |  |
| priorityClassName | string | `""` |  |
| rbac.create | bool | `true` | Specifies whether RBAC resources should be created |
| replicaCount | int | `1` |  |
| resources | object | `{}` |  |
| securityContext | object | `{}` |  |
| service.port | int | `80` |  |
| serviceAccount.automountServiceAccountToken | bool | `true` | Whether automounting API credentials for a service account |
| serviceAccount.create | bool | `true` | Specifies whether a ServiceAccount should be created |
| serviceAccount.name | string | `""` | The name of the ServiceAccount to use. If not set and create is true, a name is generated using the fullname template |
| serviceMonitor | object | `{"annotations":{},"enabled":false,"interval":"15s","labels":{},"namespace":"monitoring"}` | namespace: "ingress-apisix" |
| serviceMonitor.annotations | object | `{}` | @param serviceMonitor.annotations ServiceMonitor annotations |
| serviceMonitor.labels | object | `{}` | @param serviceMonitor.labels ServiceMonitor extra labels |
| tolerations | list | `[]` |  |
