

## Agent安装包目录说明(agent-package)

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

### jre目录说明

本目录用于存放linux/win/mac版本的jre，作为worker-agent.jar的Java执行环境。

#### 准备工作

- 请下载Linux/windows/macos对应系统的JRE1.8版本（注意收费的问题），并解压到当前目录，不要解压后的jre_xxxx目录。
- 下载加解密工具包bcprov-jdk16-1.46.jar，上一步解压出来的lib/ext目录下， 请从正规的maven仓库下载，以下地址仅供参考：
  http://central.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar

#### 重新压缩jre包

- 直接在jre里面的根目录下压缩，zip -r jre.zip *。 也就是压缩包里面不允许再有jre这级目录了。
- 生成jre.zip 放到相应操作系统类型目录下。分别是linux/macos/windows

### .agent.properties说明

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

devops.project.id 是agent绑定的项目的英文名