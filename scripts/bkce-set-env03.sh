#!/bin/bash
# 在蓝鲸社区版时, 生成所需的ci.env文件.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
CTRL_DIR="${CTRL_DIR:-/data/install}"
source $CTRL_DIR/load_env.sh
source $CTRL_DIR/bin/02-dynamic/hosts.env
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CI_SRC_DIR=${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}
set +a
ci_env_default="./bin/default/ci.env"
ci_env_03="./bin/03-userdef/ci.env"

set_env03 (){
  for kv in "$@"; do
    if ! grep -q "^${kv%%=*}=[^ ]" "$ci_env_03" 2>/dev/null; then  # 非空则不覆盖.
      echo "SET_ENV03: $ci_env_03 中未曾赋值，新增 $kv"
      [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确
，或人工修改env文件后重试。"
      # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
      grep -qxF "$kv" "$ci_env_03" 2>/dev/null || echo "$kv" >> "$ci_env_03"
      eval "$kv"  # 立即生效
    fi
  done
}

set_env03_en (){
  for kv in "$@"; do
    echo "SET_ENV03_EN: $ci_env_03 中已赋值，重新覆盖生效 $kv"
    [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确
，或人工修改env文件后重试。"
    # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
    grep -qxF "$kv" "$ci_env_03" 2>/dev/null || echo "$kv" >> "$ci_env_03"
    eval "$kv"  # 立即生效
  done
}

random_pass (){
  base64 /dev/urandom | head -c ${1:-16}
}
uuid_v4 (){
  if command -v uuidgen &>/dev/null; then
    uuidgen
  elif command -v uuid &>/dev/null; then
    uuid -v 4
  else
    echo >&2 "ERROR: no UUID v4 provider available. please install uuidgen or uuid command."
    return 0
  fi
}

cd "$CTRL_DIR"
pkg_env_tpl="$BK_PKG_SRC_PATH/ci/scripts/bkenv.properties"
if [ -f "$pkg_env_tpl" ] && ! diff -q "$pkg_env_tpl" "$ci_env_default" 2>/dev/null; then
  echo "安装包中存在新版env文件, 更新ci.env模板: $ci_env_default"
  cp -v "$pkg_env_tpl" "$ci_env_default" || echo "更新ci.env模板失败."
fi

echo "检查设置 CI 基础配置，一次生成"
set_env03 BK_HTTP_SCHEMA=http \
  BK_DOMAIN=$BK_DOMAIN \
  BK_PAAS_PUBLIC_URL=$BK_PAAS_PUBLIC_URL \
  BK_CI_AUTH_PROVIDER=bk_login_v3 \
  BK_HOME=$BK_HOME \
  BK_SSM_HOST=bkssm.service.consul \
  BK_IAM_PRIVATE_URL=$BK_IAM_PRIVATE_URL \
  BK_PAAS_FQDN=${BK_PAAS_FQDN:-${BK_PAAS_PUBLIC_ADDR%:*}} \
  BK_PAAS_HTTPS_PORT=${BK_PAAS_HTTPS_PORT:-443} \
  BK_PAAS_PRIVATE_URL=$BK_PAAS_PRIVATE_URL \
  BK_SSM_PORT=$BK_SSM_PORT \
  BK_LICENSE_PRIVATE_URL=$BK_LICENSE_PRIVATE_URL \
  BK_CI_PAAS_DIALOG_LOGIN_URL=$BK_PAAS_PUBLIC_URL/login/plain/?c_url= \
  BK_CI_PAAS_LOGIN_URL=\$BK_PAAS_PUBLIC_URL/login/\?c_url= \
  BK_CI_APP_CODE=bk_ci \
  BK_CI_APP_TOKEN=$(uuid_v4) \
  BK_CI_INFLUXDB_ADDR=$BK_CI_IP0:8086 \
  BK_CI_INFLUXDB_DB=agentMetrix \
  BK_CI_INFLUXDB_HOST=$BK_CI_IP0 \
  BK_CI_INFLUXDB_PASSWORD=$BK_INFLUXDB_ADMIN_PASSWORD \
  BK_CI_INFLUXDB_PORT=8086 \
  BK_CI_INFLUXDB_USER=admin
# 复用es7, 读取账户密码, 刷新03env.
set_env03 BK_CI_ES_REST_ADDR=$BK_ES7_IP BK_CI_ES_USER=elastic BK_CI_ES_PASSWORD=$BK_ES7_ADMIN_PASSWORD
# 复用rabbitmq, 生成密码并创建账户, 刷新03env.
set_env03 BK_CI_RABBITMQ_ADDR=$BK_RABBITMQ_IP:5672 BK_CI_RABBITMQ_USER=bk_ci BK_CI_RABBITMQ_PASSWORD=$(random_pass) BK_CI_RABBITMQ_VHOST=bk_ci
# 选择复用mysql, 生成密码并创建账户, 刷新03env.
set_env03 BK_CI_MYSQL_ADDR=${BK_MYSQL_IP}:3306 BK_CI_MYSQL_USER=bk_ci BK_CI_MYSQL_PASSWORD=$(random_pass)
# 复用redis, 读取密码, 刷新03env.
set_env03 BK_CI_REDIS_HOST=$BK_REDIS_IP BK_CI_REDIS_PASSWORD=$BK_PAAS_REDIS_PASSWORD

# 调整BK_CI_AUTH_PROVIDER及URL等
set_env03_en BK_CI_AUTH_PROVIDER=bk_login_v3 \
  BK_CI_FQDN=$(echo ${BK_PAAS_PUBLIC_ADDR}|sed "s#paas#devops#g;s#:80##g") \
  BK_CI_PUBLIC_URL=http://\$BK_CI_FQDN \
  BK_CI_REPOSITORY_GITLAB_URL=http://\$BK_CI_FQDN

if grep -w repo $CTRL_DIR/install.config|grep -v ^\# ; then
  set_env03_en BK_REPO_GATEWAY_IP=$BK_REPO_GATEWAY_IP \
  BK_REPO_HOST=$(echo ${BK_PAAS_PUBLIC_ADDR}|sed "s#paas#repo#g;s#:80##g")
fi

echo "合并env."
./bin/merge_env.sh ci &>/dev/null || true

