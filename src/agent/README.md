# Agent(Golang)

构建Agent是指包含了agent进程监控和调度部分逻辑的代码，不包含与流水线交互的构建类业务逻辑代码，需要与另外一个worker(kotlin) 一起整合才能是完整的Agent包。

## Agent二进制程序编译

> 以下命令请在本目录执行

linux系统，执行命令 `make clean build_linux`

macos系统，执行命令 `make clean build_macos`

windows编译，执行脚本 `build_windows.bat`

执行以上编译脚本或命令后，会在 `bin` 目录下生成对应的可执行文件。

比如执行 `make clean build_linux` 命令会在 `bin` 目录下生成 `devopsDaemon_linux`、`devopsAgent_linux`、`upgrader_linux` 文件，其他系统依此类推。

- devopsDaemon: 用于守护agent进程，监控和拉起agent进程
- devopsAgent: 用于和调度服务通信，以及拉起构建进程worker
- upgrader: 升级文件

## Agent控制脚本

举例Linux, 其他系统依此类推。

- scripts/linux/install.sh： agent安装脚本
- scripts/linux/start.sh： agent启动脚本
- scripts/linux/stop.sh： agent停止脚本
- scripts/linux/uninstall.sh： agent卸载脚本


## 架构设计说明
### Agent 模块说明
agent模块为GoAgent的核心模块。主要负责**执行worker以及做相关辅助工作**。
#### 模块主流程说明
![image](https://github.com/Tencent/bk-ci/blob/master/docs/resource/img/Agent%E6%A8%A1%E5%9D%97%E6%B5%81%E7%A8%8B%E5%9B%BE.png)
#### 步骤说明
针对流程图步骤补充说明，序号为流程图步骤序号。
##### 1 检查进程。
主要通过 **CheckProcess** 函数，函数主要通过输入的模块名称 例如：agent，获取runtime目录下的 name.pid 和 name.lock 文件。通过是否可以拿到 name.lock 文件锁的锁，来判断当前进程是否正在运行，如若没有在运行，则获取全局锁 total-lock.lock 来修改 name.pid 文件写入当前agent进程ID，同时一直持有 name.lock 锁直到进程退出时自动释放。

##### 2 初始化配置。
1. 从 **.agent.properties** 文件中读取agent配置信息到 **GAgentConfig** 对象中。在读取的同时会写入一次，将 GAgentConfig 对象初始化的一些值持久化到 .agent.properties中。
2. 初始化证书。 如果根目录中存在 **.cert** 文件，则将其添加至系统证书池，并添加至agent发送https请求的 **TLS** 配置中
3. 初始化Agent环境变量。读取系统一些agent可能用到的环境变量到agent **GAgentEnv** 中，方便后续使用，例如 HostName，osName等。注：windows系统在这一步中因为可能持有虚拟网卡的IP，所以在上报agentIP地址时会 **忽略被配置了忽略虚拟网卡的windows机器IP**。

##### 3 上报后台Agent启动。
**POST|env服务|agent/thirdPartyAgent/startup** 并解析返回结果，如果成功返回则执行后续步骤否则退出。

##### A 数据采集。
通过配置替换代码中写好的采集配置，启动 **telegraf** agent，并将数据上报到后台的influxDB。

##### B 心跳上报。
无限循环，10s一次。通过 **POST|env服务|agent/thirdPartyAgent/agents/newHeartbeat** 发送agent环境的一些配置到后台，并解析后台结果，修改部分agent配置。

##### C 检查升级。无限循环，20s一次。
1. 获取当前jdk版本信息
2. 通过 **POST|dispatch环境|agent/thirdPartyAgent/upgradeNew** 上报agent，worker，jdk版本信息判断是否需要升级。不需要升级则直接返回。
3. 下载最新版本应用对比md5判断是否需要升级。不需要升级则直接返回。
4. 进入升级逻辑。获取**构建锁**，获取锁后则判断当时**是否存在预处理任务或者任务实例**，存在时返回。
5. 升级JDK。通过先将新的jdk文件解压为jdk_currenttime文件，之后删除老的jdk文件夹，在将jdk_$currenttim文件改名为jdk。
6. 升级worker。删除原文件，复制新文件。
7. 升级agent。执行 **upgrader** 模块。

##### D pipeline。无限循环，30s一次。
1. 通过 **GET|env服务|agent/thirdPartyAgent/agents/pipelines** 获取需要执行的pipeline任务。
2. 通过 **PUT|env服务|agent/thirdPartyAgent/agents/pipelines** 更新任务状态。
3. 将任务脚本写入 工作空间下 **devops_pipeline_SeqId_type.sh** 文件。执行后获取输出内容，并通过更新任务状态上报后台，同时删除脚本文件。

##### E 定期清理。
无限循环，2小时一次。删除超过配置中 **LogsKeepHours**的垃圾文件（hs_err_pid前缀文件）和日志文件。

##### 4 从后台拉取构建任务。无限循环，5s一次。
1. 调用 **GET|env服务|agent/thirdPartyAgent/status** 获取agent状态，如果状态返回不正常则跳过这次任务。
2. 判断当前运行的instance是否超过配置 **ParallelTaskCount** 最大任务数，如果超过则跳过循环。
3. 获取**构建锁**，防止与其他任务差生冲突。
4. 通过 **GET|dispatch服务|agent/thirdPartyAgent/startup** 获取构建任务，如果任务为空则跳过这次。否则则添加**预处理任务**到 **GBuildManager** 对象。注：unix构建机受**非登录用户启动进程无法拿到 ~/.bashrc 中的环境变量**的影响，需要创建 prestart和start脚本，通过设置 **exec -l** 来通过当前用户启动。

##### 5 启动构建。
1. 获取工作空间下的 **worker.jar** 文件。如果没有则尝试通过获取工作空间下的 **tmp** 目录下中同名文件进行复制使用来尝试自愈，如果 tmp 目录中的worker版本不对或者不存在文件，则进行 **结束构建**逻辑。
2. 在工作空间下创建 **build_tmp** 目录，并在目录下启动 worker进程执行构建任务。任务启动后删除**预处理任务**同时添加**任务实例**到 **GBuildManager** 对象。

##### F 等待进程结束
通过 **process.Wait()** 来等待进程结束，删除**GBuildManager** 对象中的任务实例，并执行 **结束构建**逻辑。
##### G 清理构建空间，即结束构建逻辑。
- 清理构建过程中产生的文件脚本，与 build_tmp目录下超过7天的文件。
- 通过**POST|dispathc服务|agent/thirdPartyAgent/workerBuildFinish** 上报后台任务结束。



### Daemon 模块说明
daemon模块是agent模块的守护进程，主要功能是维护agent进程一直运行。
daemon模块在windows系统和unix系统中有不同的实现
#### windows实现
win实现主要是靠 **github.com/kardianos/service** 库调用的windows service功能实现，通过实现库中service接口维护agent一直运行。
#### unix实现
unix中的实现主要通过使用**定时器，5s一次**检查Agent模块进程是否存在，检测方式通过获取agent的文件锁，进程推出后文件锁会自动释放，所以如果无法获取文件锁，说明进程正常运行，如果可以获取文件锁，则说明agent进程退出了，这时Daemon会将其拉起。

### Install & Upgrade 模块说明
安装和升级模块，主要负责daemon模块和agent模块的升级。
#### install模块
安装模块功能较为简单，主要通过调用安装脚本，安装agent。
#### upgrade模块
升级模块和agent模块中的升级Agent的逻辑一致，通过杀掉Agent进程后，替换Agent文件并启动来完成升级。
