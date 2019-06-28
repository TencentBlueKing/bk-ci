# bk-ci 编译说明

## Frontend前端代码编译

### 系统要求

nodejs版本 8.0.0及以上

### 编译

- 打包并部署相应的vue工程
  依次进入到devops-nav、devops-pipeline、devops-codelib、devops-environment、devops-atomstore、devops-ticket目录，在每个目录下均

```
# 先执行
npm install --user=root && npm rebuild node-sass
# 再执行
npm run public:external
```

(如果在install时遇到无法安装node-sass和phantomjs依赖，可选择配置国内镜像源)
(如果在执行npm run public:external过程中有遇到 Cannot read property 'range' of null 的报错，可尝试将对应工程package.json中的babel-eslint依赖的版本设置成 "babel-eslint": "^8.0.1")

执行完这两条命令后，每个目录下均会生成一个dist文件夹，把相应的dist文件夹重命名后放置到相应的部署目录即可



## 网关代码

网关为一序列的lua脚本和nginx配置，所以不涉及到编译

## Agent编译(Go)

### 系统要求

- Golang 1.12
- agent 使用 golang 编写，目前支持 linux/windows/macos 三种系统，其他系统没有做编译验证。编译时需要3种系统下用go编译器编译。

### 编译

- 不要使用交叉编译生成 agent 程序。
- 安装好golang后，将 `bk-ci/src/agent` 加入 `GOPATH` 环境变量。
- Linux版本编译： build_linux.sh 
- Windows版本编译： build_windows.bat
- MacOS 版本编译： build_macos.sh
- 输出在agent/bin目录下以下文件（安装包）：

```
    |- devopsAgent.exe
    |- devopsAgent_linux
    |- devopsAgent_macos
    |- devopsDaemon.exe
    |- devopsDaemon_linux
    |- devopsDaemon_macos
    |- upgrader.exe
    |- upgrader_linux
    |- upgrader_linux
```



## backend后端微服务编译(kotlin)

### 系统要求

- MySQL 5.7
- JDK 1.8
- Gradle 4.8 - 4.10 

#### 数据库初始化

- 编译PO时用了JOOQ做DB表与PO生成映射，需要依赖于数据库表，所以数据库需要先初始化，初始化脚本在工程bk-ci/support-files/sql目录下，登录到数据库中按顺序执行即可。 

#### gradle编译前配置

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

#### 编译

- cd bk-ci/src/backend & gradle clean build
- 构建出来的产物都放在backend/release目录下，主要包含以下产物:
  - worker-agent.jar  构建机中负责运行任务的进程，用kotlin编写所以与后台放一起编译,最后会在与GoAgent安装包时一并合并部署。
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
