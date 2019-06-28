# Linux jre说明

本目录用于存放 Windows 版本的jre，作为worker-agent.jar在 Windows 下的Java执行环境。

## 准备工作
- 请下载 Windows 系统的JRE1.8版本（注意收费的问题），并解压到当前目录，不要解压后的jre_xxxx目录。
- 下载加解密工具包bcprov-jdk16-1.46.jar，上一步解压出来的lib/ext目录下， 请从正规的maven仓库下载，以下地址仅供参考：
http://central.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar

## 重新压缩jre包
- zip -r jre.zip *

