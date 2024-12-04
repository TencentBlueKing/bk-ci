# bk-ci 编译说明
## Frontend前端代码编译

蓝鲸ci前端（frontend目录下）, common-lib和svg-sprites为项目依赖的静态资源，其余目录均为vue的spa工程，其中devops-nav为主入口，其它子服务以Iframe 或 UMD 的方式接入

## 系统要求

nodejs版本 8.0.0及以上

## 安装说明

- 1、打包并部署相应的vue工程
进入到src/frontend目录下
```
# 先全局安装yarn
npm install -g yarn
# 然后执行install
yarn install
# 然后安装每个子服务的依赖
yarn start
# 最后执行打包命令
yarn public
```

## 网关代码

网关为一序列的lua脚本和nginx配置，所以不涉及到编译

## Agent编译(Go)

### 系统要求

- Golang 1.12
- agent 使用 golang 编写，目前支持 linux/windows/macos 三种系统，其他系统没有做编译验证。编译时需要3种系统下用go编译器编译。

### 编译

备注：编译macos平台可执行文件时，若不开启cgo，则会影响采集cpu和diskio数据，这是因为Telegraf 的 cpu和diskio 插件采集使用的 shirou 包需要开启cgo才可以在darwin情况下采集成功

前置条件：进入agent根目录 -> `cd bk-ci/src/agent`

- linux平台 & mac平台编译
    - 交叉编译多平台可执行文件(无cgo)：make all
    - 编译linux平台可执行文件(无cgo)：make linux
    - 编译win平台可执行文件(无cgo)：make windows
    - 编译mac平台可执行文件(无cgo)：make macos_no_cgo
    - 编译mac平台可执行文件(cgo)：make macos_cgo
- windows平台编译
    - 编译win平台可执行文件(无cgo)：build_windows.bat
- 可执行文件统一输出在agent/bin目录下：
```shell
.
├── devopsAgent.exe
├── devopsAgent_linux
├── devopsAgent_linux_arm64
├── devopsAgent_linux_mips64
├── devopsAgent_macos
├── devopsAgent_macos_arm64
├── devopsDaemon.exe
├── devopsDaemon_linux
├── devopsDaemon_linux_arm64
├── devopsDaemon_linux_mips64
├── devopsDaemon_macos
├── devopsDaemon_macos_arm64
├── installer.exe
├── installer_linux
├── installer_linux_arm64
├── installer_linux_mips64
├── installer_macos
├── installer_macos_arm64
├── upgrader.exe
├── upgrader_linux
├── upgrader_linux_arm64
├── upgrader_linux_mips64
├── upgrader_macos
└── upgrader_macos_arm64
```


## backend后端微服务编译(kotlin)

### 系统要求

- MySQL 5.7
- JDK 1.8
- Gradle 6.7

#### 数据库初始化

编译PO时用了JOOQ做DB表与PO生成映射，需要依赖于数据库表，所以数据库需要先初始化，初始化脚本在工程bk-ci/support-files/sql目录下，登录到数据库中按顺序执行即可。 

#### gradle编译前配置
gradle.properties 配置修改如下配置项:

  ```
  MAVEN_REPO_URL=修改为你的maven私库，如果有的话,没有可用公共的源
  MAVEN_REPO_SNAPSHOT_URL=修改为你的快照Maven私库，如果有的话,没有可用公共的源
  MAVEN_REPO_DEPLOY_URL= 这个是如果你需要将jar包deploy到你的maven私有库，则设置为你的地址
  MAVEN_REPO_USERNAME=需要deploy时才需要填写
  MAVEN_REPO_PASSWORD=需要deploy时才需要填写
  DB_HOST=你的数据库，编译时JOOQ需要连接数据库读取数据库表来进行生成PO编译
  DB_USERNAME=数据库用户名
  DB_PASSWORD=数据库密码
  ```

#### 编译

```shell
cd bk-ci/src/backend/ci & gradle clean build
```

构建出来的产物都放在backend/release目录下，主要包含以下产物:
包名称 | 描述
:--- | :---
worker-agent.jar |  构建机中负责运行任务的进程，用kotlin编写所以与后台放一起编译,最后会在与GoAgent安装包时一并合并部署。
boot-assembly.jar | 该包为整个单体服务Springboot包，整合10个微服务而成的单体服务，如果要采用单体包安装，则在gateway的init.lua中的service_name 配置项要设置为 "bk-ci", 如果是单体服务则该配置项置空串。默认是各个微服务分析部署。
boot-artifactory.jar  | 构件归档微服务Springboot.jar
boot-dispatch.jar     | 构建调度微服务Springboot.jar
boot-environment.jar  | 环境管理微服务Springboot.jar
boot-log.jar          | 日志微服务Springboot.jar
boot-plugin.jar       | 扩展微服务Springboot.jar
boot-process.jar      | 流水线微服务Springboot.jar
boot-project.jar      | 项目管理微服务Springboot.jar
boot-repository.jar   | 代码库微服务Springboot.jar
boot-store.jar        | 研发商店微服务Springboot.jar
boot-ticket.jar       | 凭证微服务Springboot.jar
