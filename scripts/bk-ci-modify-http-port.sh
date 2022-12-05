#!/bin/bash
# 支持蓝盾调整访问端口
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
ci_env_default="$CTRL_DIR/bin/default/ci.env"
ci_env_03="$CTRL_DIR/bin/03-userdef/ci.env"
ci_env_04="$CTRL_DIR/bin/04-final/ci.env"

for i in ${ci_env_03} ${ci_env_04} ; do sed -i "/BK_CI_HTTP_PORT/d;/BK_CI_FQDN/d;/BK_CI_PUBLIC_URL/d;/BK_CI_REPOSITORY_GITLAB_URL/d" ${i} ; done

source $CTRL_DIR/load_env.sh
source $CTRL_DIR/bin/02-dynamic/hosts.env
set +a
BK_CI_HTTP_PORT=$(awk '{print $1}' /tmp/bk_ci_http_port)

set_env03_en (){
  for kv in "$@"; do
    echo "SET_ENV03_EN: $ci_env_03 中已赋值，重新覆盖生效 $kv"
    [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确
，或人工修改env文件后重试。"
    # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
    p_d=$(echo ${kv}|awk -v FS="=" '{print $1}')
    sed -i "/${p_d}/d" "$ci_env_03"
    grep -qxF "$kv" "$ci_env_03" 2>/dev/null || echo "$kv" >> "$ci_env_03"
    eval "$kv"  # 立即生效
  done
}

cd "$CTRL_DIR"
pkg_env_tpl="$BK_PKG_SRC_PATH/ci/scripts/bkenv.properties"
if [ -f "$pkg_env_tpl" ] && ! diff -q "$pkg_env_tpl" "$ci_env_default" 2>/dev/null; then
  echo "安装包中存在新版env文件, 更新ci.env模板: $ci_env_default"
  cp -v "$pkg_env_tpl" "$ci_env_default" || echo "更新ci.env模板失败."
fi

set_env03_en BK_CI_HTTP_PORT=${BK_CI_HTTP_PORT} \
  BK_CI_FQDN=$(echo ${BK_PAAS_PUBLIC_ADDR}|sed "s#paas#devops#g;s#:80#:${BK_CI_HTTP_PORT}#g") \
  BK_CI_PUBLIC_URL=http://\$BK_CI_FQDN \
  BK_CI_REPOSITORY_GITLAB_URL=http://\$BK_CI_FQDN

echo ${BK_CI_FQDN} ${BK_CI_PUBLIC_URL} ${BK_CI_REPOSITORY_GITLAB_URL}

echo "合并env."
./bin/merge_env.sh ci &>/dev/null || true

