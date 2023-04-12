# kubernetes-manager

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

#### kubernetes-manager和bk-ci同k8s集群同namespace部署
配置bk-ci helm values
'bkCiKubernetesHost': "http://kubernetes-manager"  // 默认kubernetes-manager的service类型为 NodePort
'bkCiKubernetesToken': "landun" // 同kubernetesManager.apiserver.auth.apiToken.value配置
#### kubernetes-manager和bk-ci同集群不同namespace部署
配置bk-ci helm values
'bkCiKubernetesHost': "http://kubernetes-manager.{{ .Release.Name }}"  // 默认kubernetes-manager的service类型为 NodePort
'bkCiKubernetesToken': "landun" // 同kubernetesManager.apiserver.auth.apiToken.value配置
#### kubernetes-manager和bk-ci不同集群部署
配置bk-ci helm values
'bkCiKubernetesHost': "http://node:port"  // // 默认kubernetes-manager的service类型为 NodePort
'bkCiKubernetesToken': "landun" // 同kubernetesManager.apiserver.auth.apiToken.value配置

### 以二进制的方式启动

1. 打包二进制。参考makefile中的 build.xxx 和 release.xxx 同时修改makefile中 CONFIG_DIR，OUT_DIR来存放配置文件和目录文件（配置文件格式可参考 resources 目录）。

2. 补充说明：
    - 二进制格式启动类似直接镜像启动，可以相互参考。同时二进制格式启动一样不具备mysql和redis，需要自行准备。
