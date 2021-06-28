#!/bin/bash
# codecc启动后的初始化操作.
# 目前仅适配了蓝鲸社区版的批量脚本.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

# 注册ci-auth的回调.
echo "reg ci-auth callback."
iam_callback="${BK_PKG_SRC_PATH:-/data/src}/codecc/support-files/templates/codecc_conf.json"
./pcmd.sh -H "$BK_CI_AUTH_IP0" curl -vsX POST \
  "http://localhost:$BK_CODECC_AUTH_API_PORT/api/op/auth/iam/callback/" \
  -H "Content-Type:application/json" -d "$(
    sed "s@\\\${BK_CODECC_PUBLIC_URL}@http://bk-codecc.service.consul/codecc/@" "$iam_callback"
  )"
echo ""

