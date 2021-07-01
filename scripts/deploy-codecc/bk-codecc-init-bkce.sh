#!/bin/bash
# codecc启动后的初始化操作.
# 目前仅适配了蓝鲸社区版的批量脚本.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}
source "${CTRL_DIR:-/data/install}/load_env.sh"

# 注册ci-auth的回调.
echo "reg ci-auth callback."
iam_callback="${BK_PKG_SRC_PATH:-/data/src}/codecc/support-files/templates/codecc_conf.json"
# pssh 不能透传stdin, 这里选择ssh. 在蓝鲸社区版环境中测试可用.
timeout 5 ssh "$BK_CI_AUTH_IP0" curl -vsX POST \
  "http://localhost:$BK_CI_AUTH_API_PORT/api/op/auth/iam/callback/" \
  -H "Content-Type:application/json" -d "@/dev/stdin" < <(
    sed "s@\\\${BK_CODECC_PRIVATE_URL}@$BK_CODECC_PRIVATE_URL@" "$iam_callback"
  )
echo ""

