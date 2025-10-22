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
4. 内置的elasticsearch启动如果报`max virtual memory areas vm.max_map_count [65530] is too low`错误 , 需要手动修改内核参数 , 在 /etc/sysctl.conf 里面添加`vm.max_map_count=262144`,保存后执行`sysctl -p`即可

## 安全相关
1. 如何开启 JWT? [JWT配置文档](./jwt.md)
2. 如何独立安装 kubernetes-manager [kubernetes-manager安装文档](./kubernetes-manager.md)

## FAQ
#### values.yaml的配置可以在哪里看到?
[Helm Chart README](https://github.com/TencentBlueKing/bk-ci/blob/master/helm-charts/README.md)
#### 没有网络如何离线安装?
可以在有网络的机器上(且安装了helm和docker), 在`bk-ci-charts.tgz`所在目录中执行[bk-ci-offline-image.sh](./script/bk-ci-offline-image.sh),下载完所有镜像tar, 然后再将tar传到无网络机器中, 使用`docker load < xxx.tar` 或者 `minikube image load xxx.tar` 导入镜像
#### minikube 为什么没有使用物理机上的镜像?
minikube默认推荐driver=docker , 在这种模式下, 需要执行`minikube ssh`进入到起bash环境中([minikube命令](https://minikube.sigs.k8s.io/docs/commands/)) , 或者可以使用`driver=none`的方式安装
#### 镜像拉取次数限制怎么办?
使用你的账户登录registry-1.docker.io , 命令: `docker login registry-1.docker.io`
