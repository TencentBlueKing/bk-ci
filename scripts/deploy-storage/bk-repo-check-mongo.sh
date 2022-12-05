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

for db in bkrepo ; do
    mongo mongodb://$BK_REPO_MONGODB_USER:$BK_REPO_MONGODB_PASSWORD@$LAN_IP:27017/bkrepo <<EOF
use $db;
db;
show dbs;
show collections;
EOF
done
