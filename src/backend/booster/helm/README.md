# Helm部署

## 环境准备

* k8s集群和helm环境
* etcd(可选)
* mysql(可选）


## 使用方式

1. 添加helm仓库
    ```bash
    $ helm repo add bkee <bktbs helm repo url>
    "bkee" has been added to your repositories
    ```

2. 确认访问helm仓库正常
    ```bash
    $ helm search repo bkee/bktbs
    NAME            CHART VERSION	APP VERSION	DESCRIPTION
    bkee/bktbs      1.0.0        	1.0.0      	BlueKing Repository
    ```
   
3. 部署bkbcs
   ```bash
   $ kubectl create ns bcs-system
   ```
   对namespace bcs-system, 加上`bcs-webhook: "false"`这个label
   ```bash
   $ kubectl edit ns bcs-system
   ```
   配置[bcs-value.yaml](bcs-value.yaml)
   
   安装bcs
   ```bash
   $ helm install bcs-services bk/bcs-services -n bcs-system --version 1.21.2-ce -f bcs-value.yaml
   ```
   
   获取bcs-api-token
   ```bash
   $ export GatewayToken=$(kubectl get secret --namespace bcs-system bcs-password -o jsonpath="{.data.gateway_token}" | base64 -d)
   ```

   获取bcs-api-gateway clusterIP
   ```bash
   $ kubectl get svc -n bcs-system | grep api-gateway
   ```

4. 填写[tbs-value.yaml](tbs-value.yaml)
   * server.bcs.clusterID填写加速集群的ID, 测试时可填写当前服务集群`BCS-K8S-00027`
   * server.bcs.apiToken填写拥有加速集群操作权限的token, 测试时可填写上述获取到的的bcs-api-token
   * server.bcs.apiAddress填写bcs-api-gateway地址, 当前bcs的tls证书会绑定域名, 测试时可以填写clusterIP+http端口

5. 部署bktbs
    `config.yaml`配置请参考[./charts/bktbs/values.yaml](./charts/bktbs/values.yaml)
    ```bash
    $ helm install bktbs bkee/bktbs --namespace=tbs-system -f tbs-value.yaml
    NAME: bktbs
    ...
    ```
    
## 构建镜像和charts包指引

1. 构建docker镜像
    ```bash
    sh images/build.sh -v 1.0.0
    ```

2. 部署镜像
    ```bash
    cd charts/bktbs
    helm install bktbs . --namespace=bktbs
    ```
    
3. 构建helm charts包
    ```bash
    sh charts/build.sh -v 1.0.0
    ```