# 后端微服务部署

蓝鲸ci后端（backend目录下）有如下微服务
- auth
- repository
- generic 
- docker （如需要docker仓库服务）
- helm （如需要helm仓库服务）
- npm (如需要npm仓库服务)
- rpm (如需要rpm仓库服务)
- pypi（如需要pypi仓库服务）
- maven （如需要maven仓库服务）

## 1.服务器要求

jdk: 1.8 ,java 运行时
consul: 1.0 (服务器本地启动consul agent,并且加入到consul 服务集群)，用作配置中心与服务发现


## 微服务部署

### 2.1 设置部署环境变量

|   变量名   |  用途     |
| ------------ | ---------------- |
|BK_REPO_LOGS_DIR|bkrepo日志目录|
|BK_REPO_JVM_XMS|java进程启动占用内存大小|
|BK_REPO_ENV|部署环境,prod|test|dev|
|BK_REPO_CONSUL_SERVER_HOST|consul server host|
|BK_REPO_CONSUL_SERVER_PORT |consul server port|
|MODULE |微服务模块名称，比如auth,repository|

### 2.2 服务启动

在部署服务器上的示例/data/bkee/的主目录下

微服务启动脚本,以auth微服务为例,建立auth.sh脚本

```shell 
mkdir -p $BK_REPO_LOGS_DIR
java -server \
     -Dsun.jnu.encoding=UTF-8 \
     -Dfile.encoding=UTF-8 \
     -Xloggc:$BK_REPO_LOGS_DIR/gc.log \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=oom.hprof \
     -XX:ErrorFile=error_sys.log \
     -Xms$BK_REPO_JVM_XMS \
     -Xmx$BK_REPO_JVM_XMX \
     -jar $MODULE.jar \
     --spring.profiles.active=$BK_REPO_ENV \
     --spring.cloud.consul.host=$BK_REPO_CONSUL_SERVER_HOST \
     --spring.cloud.consul.port=$BK_REPO_CONSUL_SERVER_PORT
```
- 启动微服务：以auth微服务为例 /data/bkee/bkrepo/backend/auth.sh 

