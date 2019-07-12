# Agent(Golang)

构建Agent是指包含了agent进程监控和调度部分逻辑的代码，不包含与流水线交互的构建类业务逻辑代码，需要与另外一个worker(kotlin) 一起整合才能是完整的Agent包。



## Agent二进制程序编译

在根目录下分别有3个操作系统的编译脚本：

- build_linux.sh
- build_macos.sh
- build_windows.bat

只需要直接执行即可，比如Linux包将会在bin目录下生成对应devopsDaemon_linux,devopsAgent_linux ，其他系统依此类推。

- devopsDaemon： 用于守护agent进程，监控和拉起agent进程
- devopsAgent:  用于和调度服务通信，以及拉起构建进程worker

## Agent控制脚本

举例Linux, 其他系统依此类推。

- scripts/linux/install.sh：  agent安装脚本
- scripts/linux/start.sh：   agent启动脚本
- scripts/linux/stop.sh：   agent停止脚本
- scripts/linux/uninstall.sh：   agent卸载脚本



