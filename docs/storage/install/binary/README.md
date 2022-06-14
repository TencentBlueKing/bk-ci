# 安装部署

## 1. 部署目录说明

部署的目录遵循蓝鲸运营规范，这里举例以/data/bkee 作为主目录，用户可以自行更换，比如叫/a/b 都可以。目录层次多，需要仔细看，具体如下：

```shell
|- /data/bkee  # 蓝鲸根目录
  |- ci      # ci部署程序目录
  |- bkrepo  # bkrepo部署目录
```

具体说明下以下章节。

### 1.1 bkrepo部署目录

```shell
|- /data/bkee/bkrepo       # 程序主目录
  |- backend             # 后端程序目录
  |- frontend            # 存放的前端发布的静态资源目录
  |- gateway             # 网关配置文件及lua脚本
  |- scripts             # 部署脚本
  |- support-files       # 配置文件目录
```


## 2.服务部署

### 2.1 系统要求

- CentOS 7.X
- jdk: 1.8
- mongodb 3.6
- Consul 1.0+ [Consul安装](consul.md)


### 2.2 设置部署环境变量

|   变量名   | 用途                        |
| ------------ |---------------------------|
|WORK_DIR| bkrepo安装目录,以/data/bkee/为例 |
|BK_REPO_MONGODB_USER| mongodb 用户名               |
|BK_REPO_MONGODB_PASSWORD| mongodb密码                 |
|BK_REPO_MONGODB_ADDR| mongodb地址                 |
|BK_REPO_MONGODB_DB_NAME| mongodb数据库名               |
|BK_REPO_SERVICE_PREFIX| bkrepo服务前缀，默认为bkrepo-     |
|BK_REPO_CONSUL_SERVER_HOST| bkrepo consul服务ip         |
|BK_REPO_CONSUL_SERVER_PORT| bkrepo consul服务port       |

### 2.2 mongodb数据库初始化

将support-files/sql 目录下按文件序号顺序执行

```shell
mongo -u $BK_REPO_MONGODB_USER -p $BK_REPO_MONGODB_PASSWORD $BK_REPO_MONGODB_ADDR/$BK_REPO_MONGODB_DB_NAME init-data.js
```

### 2.2 consul配置初始化,后端配置文件渲染

涉及到配置文件里面有双"_"下划线定义的变量需要做占位符号替换，已经抽离到scripts/repo.env文件里:

- scripts/repo.env 中有对应的配置项，需要进行修改，如果遇到配置项涉及到蓝鲸的或者不会用到的，则可以保持默认配置不修改即可，修改后保存退出。

  - 修改INSTALL_PATH，这个为安装主目录，默认是/data/bkee
  - 修改MODULE变量建议不要修改，默认为bkrepo
  
```shell
cd $WORK_DIR/bkrepo/scripts
chmod +x render_tpl
./render_tpl -u -p $WORK_DIR -m bkrepo -e repo.env $WORK_DIR/bkrepo/support-files/templates/*.yaml
services=(auth repository dockerapi generic docker helm maven npm)
for var in ${services[@]};
do
    service=$BK_REPO_SERVICE_PREFIX$var
    echo $service
    curl -T $WORK_DIR/etc/bkrepo/$var.yaml http://$BK_REPO_CONSUL_SERVER_HOST:$BK_REPO_CONSUL_SERVER_PORT/v1/kv/bkrepo-config/$service/data
done
curl -T $WORK_DIR/etc/bkrepo/application.yaml http://$BK_REPO_CONSUL_SERVER_HOST:$BK_REPO_CONSUL_SERVER_PORT/v1/kv/bkrepo-config/application/data
echo "put config to consul kv success."
```


## 3 程序部署

### 3.1 网关部署

采用OpenResty作为网关服务器，部署主要分为OpenResty安装， gateway的lua和nginx配置代码部署两部分。

- [bkrepo网关部署](gateway.md)


### 3.2 前端部署

- [前端编译](frontend.md)，对编译有兴趣可以自行研究

前端配置文件渲染

```shell
cd $WORK_DIR/bkrepo/scripts
chmod +x render_tpl
./render_tpl -u -p /data/bkee -m bkrepo -e repo.env /data/workspace/frontend/ui/frontend#ui#index.html
```

### 3.3 后端微服务部署

- [后端服务部署](backend.md)



