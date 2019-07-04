# 蓝鲸持续集成平台(BK-CI)的代码结构


```
|- bk-ci
  |- docs  
  |- scripts
  |- src
    |- agent
    |- backend
    |- frontend
    |- gateway
  |- support-files
```

## 工程源码(src)

工程是混合了vue/lua/kotlin/java/go/shell 等几种语言，按分层逻辑分网关、前端、后端、Agent、流水线插件等工程

### 网关工程代码(gateway)

```
|- bk-ci/src
  |- gateway
    |- html     # 存放各种状态码的HTML标准模板，可替换
    |- lua      # 存放lua脚本
      |- *.lua  # 包含一些lua脚本，主要关注init.lua 脚本，包含了一些重要配置
      |- resty  # 包含resty公用代码，比如MD5，uuid，cookie等开源实现
    |- *.conf   # 包含各类conf配置，主要关注server.devops.conf和auth.conf配置文件
```

网关采用OpenResty，其基于Nginx与Lua的高性能Web服务器，通过lua脚本扩展实现对接Consul的微服务路由发现，以及用户鉴权身份认证的对接功能。 



### 前端代码(frontend)
```
|- bk-ci/src
  |- frontend
    |- devops-atomstore   # 研发商店 Store
    |- devops-codelib     # 代码库管理 Code
    |- devops-environment # 环境管理 Env
    |- devops-pipeline    # 流水线 Pipeline
    |- devops-ticket      # 凭证管理 Ticket
    |- devops-nav         # 顶部菜单导航 Nav
    |- svg-sprites        # 矢量图片
      
```

前端基于VUE开发。按服务模块划分目录结构。




### 后端微服务(kotlin/gradle)&Agent代码(go)

```
|- bk-ci/src
  |- agent         # agent基于go语言编写，用于在构建机上运行DevopsDaemon&DevopsAgent
  |- backend
    |- project                  # 项目微服务总目录
      |- api-project            # api定义抽象层
      |- api-project-sample     # 默认与对接不同平台有差异的部分的api定义抽象层
      |- api-project-blueking   # 对接蓝鲸特有差异api定义抽象层
      |- api-project-op         # 运营后台操作类api定义抽象层
      |- biz-project            # api和业务服务实现层，如有一些需要扩展抽象则会放到sample示例实现
      |- biz-project-blueking   # 对接蓝鲸平台的业务服务实现
      |- biz-project-sample     # 业务服务实现扩展示例，主要是示例如何扩展实现
      |- biz-project-op         # 运营后台操作类api的实现
      |- boot-process           # 构建springboot微服务包，设置依赖构建并输出到release目录
      |- model-process          # 使用JOOQ从db中动态生成的PO，表结构有变更需要clean后重新build
    |- boot-assembly            # 用于构建单体微服务，整合所有微服务的单体jar包
    |- common                   # 通用模块
      |- common-auth            # 权限模块
        |- common-auth-api      # 权限模块的接口抽象
        |- common-auth-sample   # 权限模块示例实现
        |- common-auth-blueking # 对接蓝鲸权限中心的实现
    |- dispatch    # 构建调度微服务总目录
    |- environment # 环境管理微服务总目录
    |- log         # 日志微服务总目录
    |- artifactory # 构件仓库微服务总目录
    |- process     # 流水线微服务总目录
    |- release     # 本地打包生成的目录，输出jar的目录
    |- repository  # 代码仓库微服务总目录
    |- store       # 研发商店微服务总目录
    |- ticket      # 凭证微服务总目录
    |- worker      # 构建机worker子模块
      |- worker-agent   # 构建机中的agent.jar 用于收发构建任务，可gradle依赖引入新增功能
      |- worker-api-sdk # 与后端微服务通信定义的各类api的实现和抽象
      |- worker-common  # agent.jar依赖通用实现和api抽象
      |- worker-plugin-archive # 与构件归档相关的内置任务插件的实现，被引入到agent中
      |- worker-plugin-scm     # 与拉取代码相关实现的内置git任务插件的实现，被引入到agent中
```



### 流水线插件SDK&Demo(java/maven)

该SDK是用于开发流水线插件的Java版SDK，编写出来的插件将在研发商店Store上架提供给使用者安装，最后在流水线中可以选择使用。 具体看目录里面的readme


```
|- bk-ci/src
  |- pipeline-plugin
    |- bksdk    # 插件SDK
    |- demo     # 一个流水线插件的starter示例, 后续开发插件自行管理代码，不存放到此处。
```

## 配置文件模板(support-files)

```
|- bk-ci/support-files
  |- agent-package  # 在部署初始化时需要分发到主程序目录下，用于存放agent安装包供下载
  |- file           # 在部署初始化时需要分发到application-artifactory.yml指定的目录下
  |- sql            # sql初始化脚本，在开发编译时就需要先初始化，否则JOOQ无法正常生成PO
  |- template       # 所有需要做替换部署配置/脚本文件
```

- template 目录文件说明:
  - 文件中的 #表示当前文件的相对路径分隔符号，如#etc#bkci#common.yml   ，假设蓝鲸总根目录在/data/bkee/， 则表示该文件最终要改名后最终路径是/data/bkee/etc/bkci/common.yml
  - 关于占位符号 ，则看support-files/README.md

## 安装脚本(scripts)

存放一些自动化安装替换脚本。待补充。

```
|- bk-ci
  |- scripts
    |- bkenv.properties   # 用到的一些配置变量定义，需要人工去修改 
    |- render_tpl         # shell脚本，用于帮助用户快速的替换所有配置并放到指定目录下去。
```