# JDK包制作说明（4.0版本以前叫JRE，但其实是JDK，命名不重要，往下看）

本目录用于存放 MacOS 系统(M系列芯片)版本的JDK，作为作为worker-agent.jar在 MacOS 下的Java执行环境。

## 准备工作
- 请下载 MacOS 系统(M系列芯片，aarch64)的JDK-17版本，建议下载腾讯KonaJDK17：https://github.com/Tencent/TencentKona-17/releases 。
- 将下载好的解压到当前目录
- 注意确认一下mac解压之后的JDK下一级目录结构是Contents/Home，与Linux不同

## 重新压缩jre包
- zip -r jdk17.zip *

