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

patt_api_success='"status" *: *0 *,'
# USAGE: api_helper CURL_ARGS...
api_helper (){
  local resp
  echo "execute on $TARGET_HOST: curl -sSf $*."
  resp=$(ssh -o ConnectTimeout=3 "${TARGET_HOST}" -- curl -sSf "$@")
  echo "resp=$resp."
  if grep -Eq "$patt_api_success" <<< "$resp"; then
    echo "request succeed."
  else
    echo "ERROR: failed to request, abort."
    return 1
  fi
}

# 注册ci-auth的回调.
echo "reg ci-auth callback."
iam_api_url="http://127.0.0.1:$BK_CI_AUTH_API_PORT/api/op/auth/iam/callback/"
iam_json_tpl="${BK_PKG_SRC_PATH:-/data/src}/codecc/support-files/templates/codecc_conf.json"
# 在中控机渲染json, stdin透传. 在蓝鲸社区版环境中测试可用.
TARGET_HOST="$BK_CI_AUTH_IP0" api_helper -m 5 -X POST -H "Content-Type:application/json" \
  -d "@/dev/stdin" "$iam_api_url" < <(
    sed "s@\\\${BK_CODECC_PRIVATE_URL}@$BK_CODECC_PRIVATE_URL@" "$iam_json_tpl"
  )

