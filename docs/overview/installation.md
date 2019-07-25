# 安装部署

## 1. 部署目录说明

部署的目录遵循蓝鲸运营规范，这里举例以/data/bkee 作为主目录，用户可以自行更换，比如叫/a/b 都可以。目录层次多，需要仔细看，具体如下：

```
|- /data/bkee  # 蓝鲸根目录
  |- bkci      # bkci部署程序目录
  |- etc       # 蓝鲸配置文件总目录
    |- bkci    # bkci配置文件目录
```

具体说明下以下章节。

### 1.1 bkci部署目录

```
|- /data/bkee/bkci       # 程序主目录
  |- agent-package       # 提供下载的agent的安装包位置
  |- frontend            # 存放的前端发布的静态资源目录
  |- gateway             # 网关配置文件及lua脚本
  |- project             # 一共有10个微服务目录，不一一列举了
    |- project.sh        # project微服务启动脚本
    |- boot-project.jar  # Project微服务的SpringBoot.jar
```

### 1.2 bkci配置文件目录

```
|- /data/bkee/etc   # 蓝鲸配置文件总目录
  |- bkci 				  # bkci配置文件目录
    |- common.yml   # 所有微服务通用配置
    |- application-project.yml  # project微服务的配置,有10个微服务配置，如有增加微服务也放此处
```

## 2. 基础环境部署

### 2.1 系统要求

- CentOS 7.X
- jdk: 1.8
- gradle: 4.8
- redis: 2.8.17
- mysql 5.7
- es: 5.6
- rabbitmq: 3.7.15 [rabbitMQ 安装文档](../install/rabbitmq.md) 
- Consul 1.0+ [Consul安装](../install/consul.md)

### 2.2 数据库初始化

将support-files/sql 目录下按文件序号顺序执行



## 3 程序部署

### 3.1 网关部署
采用OpenResty作为网关服务器，部署主要分为OpenResty安装， gateway的lua和nginx配置代码部署两部分。

- [bk-ci网关部署](../install/gateway.md)

### 3.2 前端编译部署

- [前端部署](../install/frontend.md)

前端构建之后生成的模板配置文件变量替换
```bash
  ./render_tpl -m bkci /data/bkee/bkci/frontend/pipeline/frontend#pipeline#index.html
  ./render_tpl -m bkci /data/bkee/bkci/frontend/console/frontend#console#index.html
```

### 3.3 后端微服务部署

后端微服务与agent.jar的编译和部署

- [后端服务编译及部署](../install/backend.md)

### 3.4 Agent 编译部署

- [Agent编译及安装包部署](../install/agent.md)

### 3.5 support-files/template配置文件初始化

涉及到配置文件里面有双"_"下划线定义的变量需要做占位符号替换，已经抽离到scripts/bkenv.properties文件里:

- scripts/bkenv.properties 中有对应的配置项，需要进行修改，如果遇到配置项涉及到蓝鲸的或者不会用到的，则可以保持默认配置不修改即可，修改后保存退出。

  - 修改INSTALL_PATH，这个为安装主目录，默认是/data/bkee
  - 修改MODULE变量建议不要修改，默认为bkci

- 执行scripts/render_tpl 脚本将自动将所有support-files/templates下的所有文件变量替换掉并移到正常安装路径下。

  ```bash 
  cd /data/bkee/bk-ci/scripts
  chmod +x render_tpl 
  ./render_tpl -m bkci ../support-files/templates/*
  ```

  

