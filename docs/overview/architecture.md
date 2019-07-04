# 蓝鲸持续集成平台(BK-CI)架构设计

![Architecture](../resource/img/architecture.png)

蓝鲸持续集成平台(简称**bkci** )是基于 kotlin/java/js/go/lua/shell等多种语言编写实现的，采用完全前后分离，插件式开发，具备高可用可扩展的服务架构设计：

- **前端&接口网关(WebAPI Gateway & FrontEnd) ：**

  - **WebAPI Gateway：** 由OpenResty负责，包含了对接用户登录及身份鉴权，和后端API的**Consul**服务发现转发的lua脚本及Nginx配置
  - **FrontEnd：** 基于VUE的纯前端工程，包含一序列的js,img和html等静态资源。

- **后端服务(MicroService BackEnd)：** 基于Kotlin/Java编写，采用SpringCloud框架的微服务架构设计，以下按各微服务模块的启动顺序介绍：

  - **Project：** 项目管理，负责管理流水线的项目，多个模块依赖于此。
  - **Log：** 构建日志服务，负责接收构建的日志的转发存储和查询输出。
  - **Ticket：** 凭证管理服务，存储用户的凭证信息，比如代码库帐号密码/SSL/Token等信息。
  - **Repository:** 代码库管理服务，存储用户的代码库，依赖于Ticket的联动。
  - **Artifactory:** 制品构件服务，该服务只实现了简化版的存取构件功能，可扩展对接自己的存储系统。
  - **Environment:** 构建机服务，导入构建机以及用环境管理构建机集群用于构建调度的并发。
  - **Store：** 研发商店服务，负责管理流水线扩展插件和流水线模块功能，包括插件&模板升级上下架，与process和artifactory联动。
  - **Process：** 流水线管理，负责管理流水线以及流水线编排调度功能的核心服务。
  - **Dispatch：** 构建（机）调度，负责接收流水线的构建机启动事件，分发给相应构建机处理。
  - **Plugin：** 服务的插件扩展服务，目前为空，主要是用于提供给后续扩展一些与前端页面联动的后台服务，比如对接各类CD平台，测试平台，质量检查平台等等，与前端页面配置，想象空间很大。

- **资源服务层(Resource)：** 包括提供存储和必须的基础中间件等。
  - **Storage(存储服务):** 存储服务/中间件等一序列依赖的基础环境。
    - **MySQL/MariaDB：** bkci的主数据库存储，可用mysql 5.7.2 /mariadb 10.x存储以上所有微服务的关系型数据。
    - **Redis：** 核心服务缓存，3.x版本，缓存构建机信息和构建时的信息和提供分布式锁操作等等。
    - **ElasticSearch：** 日志存储，log模块对接ES来对构建的日志做存取。
    - **RabbitMQ：** 核心消息队列服务，bkci的流水线事件机制是基于RabbitMQ来流转事件消息的。
    - **FileSystem：** 这块主要为artifactory提供服务，用于存储插件，构建产物等二进制文件服务，可对接文件或者云存储类，扩展在artifactory服务模块。
    - **Consul：** 作为微服务的服务发现Server，需要搭建Consul Server， 以及在bkci微服务部署的所在机器上同时安装Consul并以 Agent方式运行。  组建集群可以直接用bkci微服务部署机器(2台)上直接以consul server和agent方式直接启动，以减少对机器数的需求。

  - **Agent(构建机):**   构建机是负责运行CI打包编译构建的一台服务器/PC，是由比如go，gcc，java，python，nodejs等等编译环境依赖，再加上运行由bkci提供编写实现的两部分服务进程：
    - **Agent：** 由Golang编写实现，分DevopsDaemon和DevopsAgent两个进程 ：
      - **DevopsDaemon：** 负责守护和启动DevopsAgent。
      - **DevopsAgent：** 负责与**Dispatch**和**Environment**微服务通信，负责整个**Agent**的升级和**Worker**(任务执行器) 进程的启动和销毁工作。
    - **Worker：** 由Kotlin编写实现，是一个命名为agent.jar的文件，，任务真正的执行者。被**DevopsAgent**通过jre来拉起运行，之后会负责与**Process微服务模块**通信，领取插件任务并执行和上报结果(**Log&Process**)。
