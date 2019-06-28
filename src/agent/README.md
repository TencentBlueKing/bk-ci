# Agent 代码说明文档

## agent 代码说明
agent 使用 golang 编写，目前支持 linux/windows/macos 三种系统，其他系统没有做编译验证。

为方便编译，所有编译依赖已放入源代码的 vendor 文件夹

## agent 编译说明
建议使用 `golang 1.12` 版本编译。

建议不要使用交叉编译生成 agent 程序。

安装好golang后，将 `bk-ci/agent` 加入 `GOPATH` 环境变量，执行 `bk-ci/agent` 下面不同操作系统对应的编译脚本，会在 `bk-ci/agent/bin` 文件夹下生成agent程序。

## 部署
agent的部署请参考配套文档
