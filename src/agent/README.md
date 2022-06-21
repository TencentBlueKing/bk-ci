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



