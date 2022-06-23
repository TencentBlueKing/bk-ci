#!/bin/bash
# 调整蓝盾端口修改后的，paas页面访问链接修改
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
CTRL_DIR="${CTRL_DIR:-/data/install}"
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CI_SRC_DIR=${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}
source $CTRL_DIR/load_env.sh
set +a

source $CTRL_DIR/load_env.sh

# 修改蓝鲸，应用列表导航
if [[ $(mysql --login-path=mysql-paas -NBe "select external_url from open_paas.paas_app where code='bk_ci'") == "$BK_CI_PUBLIC_URL" ]] ; then
    echo "open_paas ci web entrance same as BK_CI_PUBLIC_URL , no need modify 1st" && exit 0
else
    mysql --login-path=mysql-paas -Be "update open_paas.paas_app set external_url='$BK_CI_PUBLIC_URL' where code='bk_ci'"
    if [[ $(mysql --login-path=mysql-paas -NBe "select external_url from open_paas.paas_app where code='bk_ci'") == "$BK_CI_PUBLIC_URL" ]] ; then
        echo "open_paas ci web entrance modify success 2nd" && exit 0
    else
        echo "open_paas ci web entrance modify failed 2nd" && exit 2
    fi
fi

