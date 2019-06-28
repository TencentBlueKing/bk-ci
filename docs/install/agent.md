# Agent部署

Agent包含了backend的worker-agent.jar 和 agent(go) 两部分。


## agent(go) 代码说明
agent 使用 golang 编写，目前支持 linux/windows/macos 三种系统，其他系统没有做编译验证。

为方便编译，所有编译依赖已放入源代码的 vendor 文件夹

##  agent 编译生成安装包
- 建议使用 `golang 1.12` 版本编译。

- 建议不要使用交叉编译生成 agent 程序。

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



## Agent安装包的部署

在示例/data/bkee/bkci的主目录下：

- 将源码包中的support-files/agent-package目录复制到位置 /data/bkee/bkci/agent-package,

- 该目录路径由/data/bkee/etc/bkci/application-environment.yml 中指定的。请确认路径是否一致。
- 请按以下要求存放相应的安装包：

  - Agent安装包目录说明(agent-package)

```
|- agent-package # 提供下载的agent的安装包位置
  |- config   
    |- .agent.properties   # agent配置文件
  |- jar
    |- worker-agent.jar  #  从backend/worker 子模块编译出来的release/worker-agent.jar
  |- jre  
    |- linux
      |- jre.zip   # Linux版的jre1.8的压缩包，lib/ext要加入bcprov-jdk16-1.46.jar
    |- windows
      |- jre.zip   # windows版的jre1.8的压缩包，lib/ext要加入bcprov-jdk16-1.46.jar
    |- macos
      |- jre.zip  # macos版的jre1.8的压缩包，lib/ext要加入bcprov-jdk16-1.46.jar
  |- upgrade      # 存放goAgent的升级包，由agent工程构建出来的相关包存放到此处
    |- devopsAgent.exe
    |- devopsAgent_linux
    |- devopsAgent_macos
    |- devopsDaemon.exe
    |- devopsDaemon_linux
    |- devopsDaemon_macos
    |- upgrader.exe
    |- upgrader_linux
    |- upgrader_linux
  |- script    # 存储agent的安装启停控制脚本
    |- linux
    |- windows
    |- macos
```

### Agent 安装包部署

Agent 是由Go编译出来的devopsDaemon和devopsAgent  以及backend/release/worker-agent.jar 组成的。

- 将后台编译出来的backend/release/worker-agent.jar 复制到 jar目录下
- 将go编译出来的agent/bin目录下的devopsDaemon、devopsAgent、upgrader 放到upgrade



#### jre目录说明

本目录用于存放linux/win/mac版本的jre，作为worker-agent.jar的Java执行环境。

##### 准备生成jre.zip工作

- 请下载Linux/windows/macos对应系统的JRE1.8版本（注意收费的问题），并解压到当前目录，不要解压后的jre_xxxx目录。
- 下载加解密工具包bcprov-jdk16-1.46.jar，上一步解压出来的lib/ext目录下， 请从正规的maven仓库下载，以下地址仅供下载参考：[bcprov-jdk16-1.64.jar下载](http://central.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar)

##### 重新压缩jre包

- 直接在jre里面的根目录下压缩，zip -r jre.zip *。 也就是压缩包里面不允许再有jre这级目录了。
- 生成jre.zip 放到相应操作系统类型目录下。分别是linux/macos/windows

#### .agent.properties说明

一台构建机器只能安装一个agent，这个agent同时只能属于一个项目。

- Agent配置文件：config/.agent.properties文件内容
```
devops.project.id=##projectId##
devops.agent.id=##agentId##
devops.agent.secret.key=##agentSecretKey##
landun.gateway=##gateWay##
devops.parallel.task.count=4
landun.env=##landun.env##
devops.master.restart.hour=0
```

- devops.project.id 是agent绑定的项目的英文名，用户下载安装时会自动替换

- devops.agent.id 是Agent的ID，用户下载安装时会自动替换
- devops.agent.secret.key Agent的密钥，用户下载安装时会自动替换
- devops.parallel.task.count 构建并发数量，默认4个构建并发
- landun.gateway 蓝盾网关，用户下载安装时会自动替换
- landun.env 环境类型，用户下载安装时会自动替换