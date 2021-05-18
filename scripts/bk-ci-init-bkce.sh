#!/bin/bash
# ci启动后的初始化操作.
# 目前仅适配了蓝鲸社区版的批量脚本.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

# 注册ci-auth的回调.
echo "reg ci-auth callback."
iam_callback="support-files/ms-init/auth/iam-callback-resource-registere.conf"
./pcmd.sh -H "$BK_CI_AUTH_IP0" curl -vsX POST "http://localhost:$BK_CI_AUTH_API_PORT/api/op/auth/iam/callback/" -H "Content-Type:application/json" -d @${BK_PKG_SRC_PATH:-/data/src}/ci/support-files/ms-init/auth/iam-callback-resource-registere.conf
echo ""
echo "reg store image for bkci."
# 注册初始镜像到研发商店.
./pcmd.sh -H "$BK_CI_STORE_IP0" curl -vsX POST "http://127.0.0.1:${BK_CI_STORE_API_PORT:-21918}/api/op/market/image/init"
echo ""

