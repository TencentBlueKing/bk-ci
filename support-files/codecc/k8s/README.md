## 概述
[Tencent/bk-ci](https://github.com/Tencent/bk-ci) 的k8s构建工具

## 目录
```
_docs/: 相关文档

base_image/: 基础镜像
    jdk/: jdk目录
    linux/: linux的一些工具
    dockerfile/: 基础镜像的Dockerfile
    build.sh: 构建脚本

code_image/: 代码镜像
    backend/: 后端构建
    gateway/: 网关构建
    build.sh: 构建脚本

deploy_yaml/: k8s部署
    base/: 基础服务部署
        consul-nfs.yaml: consul的挂载
        consul-server.yaml: consul-server的启动
        ingress-nginx: 定制化的ingress-nginx的helm
        ingress.example.yaml: ingress配置模板
        deploy.sh:部署脚本
    business/: 业务服务部署
        backend/: 后端部署
        gateway/: 前端部署
        deploy.sh: 部署脚本

env.example.properties: 变量配置模板
```

## 注意
ingress和volume挂载(这里用的是nfs)可以跟其他业务统一处理, 这里只是作为DEMO方案展示

## 环境准备:
1. 搭建好k8s集群
2. 安装mysql5.7, redis2.8, rabbitmq3.8, es7, consul1.8
3. 找一台物理机器安装nfs
    - 共享 /data/nfs目录 
    - mkdir /data/nfs/consul
    - mkdir /data/nfs/artifactory
    - mkdir /data/nfs/agent-package
4. 所有宿主机需要安装: nfs-common , nfs-utils
5. 在code_image目录下: 
    - 到https://github.com/Tencent/bk-ci/releases/ 下载最新的release包, 解压
    - 将 bkci/support-files/file拷贝到 nfs服务器的/data/nfs/artifactory/file
    - 在 nfs服务器的/data/nfs/agent-package/中,创建jre/目录(目录结构参考https://github.com/Tencent/bk-ci/tree/master/support-files/agent-package/jre)
    - 在 nfs服务器的/data/nfs/agent-package/中,创建packages/目录(目录结构参考https://github.com/Tencent/bk-ci/tree/master/support-files/agent-package/packages) 
6. 在nfs服务器/data/nfs/agent-package/的jre和packages文件夹中, 根据每个目录下的README.md , 将jre.zip和unzip.exe准备好(可以在Windows的git的/usr/bin里面找到)
7. 复制env.example.properties,命名为env.properties,修改为自己想要的变量
8. 复制deploy_yaml/base/ingress.example.yaml, 命名为ingress.yaml, 修改自己为自己的域名
9. 下载consul到base_image/linux/目录下(确保base_image/linux/consul可执行)
10. 下载jdk8到 base_image/jdk/目录下 (确保base_image/jdk/bin/java可执行 , 推荐使用https://github.com/Tencent/TencentKona-8/releases)


## 打包基础镜像
1. 确认本地有安装docker
2. 进入base_image目录
3. 执行build.sh

## 部署基础服务
1. 确定本地安装有helm3 , kubectl(已经配好集群)
2. 进入deploy_yaml/base 
3. 执行deploy.sh

## 打包代码镜像
1. 确认本地有安装docker,kubectl(已经配好集群)
2. 打开code_image目录 
3. 修改 bkci/scripts/bkenv.properties 
    - INSTALL_PATH和MODULE不要修改(否则镜像挂载会有问题)
    - 其他的跟自己的环境对应上
4. 执行build.sh
5. 根据bkci/scripts/bkenv.properties 变量 , 在rabbitmq中执行:
    - rabbitmqctl add_vhost ${RABBITMQ_VHOST}
    - rabbitmqctl set_permissions -p ${RABBITMQ_VHOST} ${RABBITMQ_USERNAME} ".*" ".*" ".*"
6. rabbitmq需要安装rabbitmq_delayed_message_exchange插件:
    - 网址 : https://github.com/rabbitmq/rabbitmq-rtopic-exchange/releases
    - 放到rabbitmq的plugins目录下 , 然后执行```rabbitmq-plugins enable rabbitmq_delayed_message_exchange```


## 部署业务服务
1. 确定本地安装有helm3 , kubectl(已经配好集群)
2. 进入deploy_yaml/business
3. 根据自己的需要,复制values.example.yaml到values.yaml, 修改values.yaml的属性
4. 执行deploy.sh

## 关于dockerhost
1. dockerhost是管理docker容器的一个agent , 目前的方案是将宿主机的docker挂载到dockerhost的pod里面 , 且pod使用hostNetwork
2. dockerhost必须手动插入数据表devops_ci_dispatch.T_DISPATCH_PIPELINE_DOCKER_IP_INFO , 防止恶意注册 , ip为宿主机IP

## DNS
1. 外部服务和内部服务的相互调用 , 采用dns来处理
2. 可以使用dnsmasq搭建dns服务, 将k8s和其他相关的物理机的resolve指向这台dns
3. 这台dns要配置好服务的域名的host
4. 注意 , 若docker容器连接不上网络 , 有可能是网络配置有问题(如没有docker0等)

## 其他
1. [架构](_docs/架构.md)
2. [镜像分层](_docs/镜像分层.md)
3. [对原项目的兼容](_docs/对原项目的兼容.md)