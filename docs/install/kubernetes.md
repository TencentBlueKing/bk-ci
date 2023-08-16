# Kubernetes

Kubernetes (K8s) 是一个开源系统，用于自动化容器化应用程序的部署、扩展和管理。

在 Kubernetes 集群上部署BK-CI , 有利于 BK-CI 的动态扩展 。接下来你将看到在 Kubernetes 集群上部署 BK-CI 的步骤。

## 操作系统
LINUX

## 依赖
1. [Kubernetes](https://kubernetes.io/) (个人用户推荐 [Minikube](https://minikube.sigs.k8s.io/docs/start/))
2. [Helm3](https://helm.sh/docs/intro/install/)

## 部署步骤
1. 从 [Releases](https://github.com/TencentBlueKing/bk-ci/releases) 下载最新版本的 bk-ci-charts.tgz
2. 执行 `helm install <ReleaseName> bk-ci-charts.tgz` 安装
    - 若集群没有ingress-controller , 需要再加上 `--set nginx-ingress-controller.enabled=true` 参数
    - 若集群已经有ingress-controller , 则根据controller类型修改 `ingress.annotations.kubernetes.io/ingress.class` , 默认为nginx
3. 安装成功后, 你可以在浏览器上访问 `devops.example.com` (需要修改本地hosts, 配置域名到ingress ip)

## FAQ
#### values.yaml的配置可以在哪里看到?
[Helm Chart README](https://github.com/TencentBlueKing/bk-ci/blob/master/helm-charts/README.md)
