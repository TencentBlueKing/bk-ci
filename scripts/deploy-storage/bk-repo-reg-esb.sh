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

echo "注册 蓝鲸 ESB"
./bin/add_or_update_appcode.sh "$BK_REPO_APP_CODE" "$BK_REPO_APP_TOKEN" "蓝盾" "mysql-paas"  # 注册app。第4个参数即是login-path。

if mysql --login-path=mysql-paas -NBe "select * from open_paas.esb_app_account where app_code='bk_repo';"|grep bk_repo ; then
    echo "bk_repo esb token register successed"
else
    echo "bk_repo esb token register failed" && exit 1
fi
