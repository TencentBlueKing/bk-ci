# 后端微服务部署

蓝鲸ci后端（backend目录下）共有10个微服务（artifactory, dispatch, environment, log, plugin, process, project, repository, store, ticket)和一个Agent（worker)

## 1.系统要求

jdk: 1.8
gradle: 4.8
redis: 2.8.17
mysql 5.7
es: 5.6
consul: 1.0 [Consul安装](consul.md)
rabbitmq: 3.7.15 [rabbitmq部署](rabbitmq.md)

## 2.安装说明

### 2.1 编译

#### 2.1.1 数据库初始化

- 编译PO时用了JOOQ做DB表与PO生成映射，需要依赖于数据库表，所以数据库需要先初始化，初始化脚本在工程bk-ci/support-files/sql目录下，登录到数据库中按顺序执行即可。 

#### 2.1.2 gradle编译前配置

- gradle.properties 配置修改如下配置项:

  ```
  MAVEN_REPO_URL=修改为你的maven私库，如果有的话,没有可用公共的源
  MAVEN_REPO_DEPLOY_URL= 这个是如果你需要将jar包deploy到你的maven私有库，则设置为你的地址
  MAVEN_REPO_USERNAME=需要deploy时才需要填写
  MAVEN_REPO_PASSWORD=需要deploy时才需要填写
  DB_HOST=你的数据库，编译时JOOQ需要连接数据库读取数据库表来进行生成PO编译
  DB_USERNAME=数据库用户名
  DB_PASSWORD=数据库密码
  ```

#### 2.1.3 开始gradle编译 

- cd bk-ci/src/backend & gradle clean build
- 构建出来的产物都放在backend/release目录下，主要包含以下产物:
  -  worker-agent.jar  构建机中负责运行任务的进程，用kotlin编写所以与后台放一起编译,最后会在与GoAgent安装包时一并合并部署。
  - boot-assembly.jar   
    - 该包为整个单体服务Springboot包，整合10个微服务而成的单体服务，如果要采用单体包安装，则在gateway的init.lua中的service_name 配置项要设置为 "bk-ci", 如果是单体服务则该配置项置空串。默认是各个微服务分析部署。
  - boot-artifactory.jar       构件归档微服务Springboot.jar
  - boot-dispatch.jar          构建调度微服务Springboot.jar
  - boot-environment.jar  环境管理微服务Springboot.jar
  - boot-log.jar                   日志微服务Springboot.jar
  - boot-plugin.jar              扩展微服务Springboot.jar
  - boot-process.jar           流水线微服务Springboot.jar
  - boot-project.jar            项目管理微服务Springboot.jar
  - boot-repository.jar      代码库微服务Springboot.jar
  - boot-store.jar               研发商店微服务Springboot.jar
  - boot-ticket.jar              凭证微服务Springboot.jar

### 2.2 微服务部署

在部署服务器上的示例/data/bkee/的主目录下，创建一个bkci目录。

- 根据微服务名称，在bkci创建对应微服务的目录, 将上述编译后的boot-xxx.jar放入，
- 将 /bk-ci/support-files/templates/目录下的各个服务名脚本，比如#project#project.sh 项目管理微服务改名为project.sh放到对应目录，
- 配置文件中双_下划线定义的变量需要替换，见[support-files占位符声明](../../support-files/README.MD)来替换相应的占位符号

```
|- /data/bkee
  |- etc
    |- bkci
      |- common.yml                # 通用配置文件
      |- application-project.yml   # 微服务配置文件
  |- bkci                  # 程序主目录
    |- project             # 微服务目录，一共有10个，不一一列举了
      |- project.sh        # project微服务启动脚本
      |- boot-project.jar  # Project微服务的SpringBoot.jar
```

- 启动微服务： 比如项目管理 /data/bkee/bkci/project/project.sh start

### 2.3 构件服务Artifactory特殊部署操作

-  如果微服务是部署多机节点，则application-artifactory.yml配置文件中的archiveLocalBasePath目录需要做成分布式高可用的，比如NFS，CephFS挂载。

- 涉及到默认插件的图标文件初始化，需要将 bk-ci/support-files/file所有目录都放到 application-artifactory.xml 文件中archiveLocalBasePath配置指定的路径目录下，否则图标展示将会有问题。

  