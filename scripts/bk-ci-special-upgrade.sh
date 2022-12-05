#!/bin/bash

set -a
CTRL_DIR="/data/install"
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CI_SRC_DIR=${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}

# 强制安装蓝鲸社区版提供的kona jdk, 规避用户自定义java导致的异常.
echo "check and install Tencent KonaJDK."
if /usr/bin/java -version | grep -F "Tencent Kona 8."; then
  echo "Tencent KonaJDK was installed."
else
  $CTRL_DIR/bin/install_java.sh -p "$BK_HOME" -f $BK_PKG_SRC_PATH/java8.tgz
fi
yum install -y fontconfig  # v1.5.7前project需要读写字体.

echo "projs is agentless artifactory auth dispatch dispatch-docker dockerhost environment gateway image log misc notify openapi plugin process project quality repository store ticket turbo websocket."
${BK_CI_SRC_DIR}/scripts/bk-ci-upgrade.sh agentless artifactory auth dispatch dispatch-docker dockerhost environment gateway image log misc notify openapi plugin process project quality repository store ticket turbo websocket

