#!/bin/bash

set -eu
trap "on_ERR;" ERR
on_ERR (){
    local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
    echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_REPO_SRC_DIR=${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}
cd "$CTRL_DIR"
source ./functions

pcmd (){
    local PCMD_TIMEOUT=${PCMD_TIMEOUT:-1200}
    timeout "$PCMD_TIMEOUT" "$CTRL_DIR/pcmd.sh" "$@" || {
        local ret=$?
        [ $ret -ne 124 ] || echo "pcmd 执行超时(PCMD_TIMEOUT=${PCMD_TIMEOUT})"
        echo "$BASH_SOURCE:$BASH_LINENO 调用pcmd时返回 $ret，中控机调试命令如下:"
        printf " %q" "$CTRL_DIR/pcmd.sh" "$@"
        printf "\n"
        return $ret
    }
}

for db in bkrepo ; do
    pcmd -H $BK_MONGODB_IP0 "mongo mongodb://$BK_REPO_MONGODB_USER:$BK_REPO_MONGODB_PASSWORD@$LAN_IP:27017/bkrepo <<EOF
use $db;
db;
show dbs;
show collections;
EOF"
done
