## 蓝盾dispatch-kubernetes schedule plugin 设计

### 主要设计：

#### 资源：

1. 构建机曾经调度过的历史节点 3个
2. 构建机曾经使用过得真实资源 5次

#### 调度策略：

1. 在可以满足的情况下，节点最少满足real作为最低资源需求，都不满足选用request，如果可以满足limit则优先满足limit
2. 在满足**策略1**的情况下，选择历史节点中最接近的

#### 调度方法：

1. 将3个历史节点从最近到最远依次打分 30 - 10分
2. 将 limit real(5个) request 打分 70 - 0分，request因为有

> 注：real: pod真实占用负载,根据历史负载统计

### 部署流程：

总体流程参考 scheduler-plugin 对应kubernetes大版本tag的 /doc/install.md 中描述的打包流程

目前插件打包部署流程如下：

1. makefile release-image 打包镜像
2. 仿照 scheduler-plugin 修改 /etc/kubernetes/manifests/kube-scheduler.yaml。例如如下所示，其中image替换为当前kuberentes版本的插件镜像。--config替换为当前目录manifests下bkdevops-scheduler-plugin.yaml。
    ```diff
    16d15
    <     - --config=/etc/kubernetes/sched-cc.yaml
    17a17,18
    >     - --kubeconfig=/etc/kubernetes/scheduler.conf
    >     - --leader-elect=true
    19,20c20
    <     image: k8s.gcr.io/scheduler-plugins/kube-scheduler:v0.23.10
    ---
    >     image: k8s.gcr.io/kube-scheduler:v1.23.10
    ```

### 参考插件：

- [scheduler-plugin](https://github.com/kubernetes-sigs/scheduler-plugins)