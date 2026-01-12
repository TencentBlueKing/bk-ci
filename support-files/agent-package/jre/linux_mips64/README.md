# JDK包制作说明（4.0版本以前叫JRE，但其实是JDK，命名不重要，往下看）

本目录用于存放 Linux 系统(MIPS64le)版本的JDK，作为作为worker-agent.jar在 MacOS 下的Java执行环境。

## 准备工作
- 请下载 Linux 系统(MIPS64le)的JDK-17版本，目前暂没有好的推荐
- 将下载好的解压到当前目录

## 重新压缩jre包
- zip -r jdk17.zip *

