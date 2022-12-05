#!/bin/bash
# 在蓝鲸社区版时, 生成所需的codecc.env文件.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
CTRL_DIR="${CTRL_DIR:-/data/install}"
source $CTRL_DIR/load_env.sh
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CODECC_SRC_DIR=${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}
set +a
codecc_env_default="./bin/default/codecc.env"
codecc_env_03="./bin/03-userdef/codecc.env"

set_env03 (){
  for kv in "$@"; do
    grep -qxF "$kv" "$codecc_env_default" 2>/dev/null || echo "$kv" >> "$codecc_env_default"
    if ! grep -q "^${kv%%=*}=[^ ]" "$codecc_env_03" 2>/dev/null; then  # 非空则不覆盖.
      echo "SET_ENV03: $codecc_env_03 中未曾赋值，新增 $kv"
      [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确
，或人工修改env文件后重试。"
      # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
      grep -qxF "$kv" "$codecc_env_03" 2>/dev/null || echo "$kv" >> "$codecc_env_03"
      eval "$kv"  # 立即生效
    fi
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
pkg_env_tpl="$BK_PKG_SRC_PATH/codecc/scripts/deploy-codecc/codecc.properties"
if ! diff -q "$pkg_env_tpl" "$codecc_env_default" 2>/dev/null; then
  echo "安装包中存在新版env文件, 更新codecc.env模板: $codecc_env_default"
  cp -v "$pkg_env_tpl" "$codecc_env_default" || echo "更新codecc.env模板失败."
fi

echo "检查设置 CODECC 基础配置"
set_env03 BK_HTTP_SCHEMA=http \
  BK_DOMAIN=$BK_DOMAIN \
  BK_PAAS_PUBLIC_URL=$BK_PAAS_PUBLIC_URL \
  BK_CODECC_AUTH_PROVIDER=bk_login_v3 \
  BK_CODECC_FQDN=codecc.\$BK_DOMAIN \
  BK_HOME=$BK_HOME \
  BK_CODECC_PUBLIC_URL=http://\$BK_CODECC_FQDN \
  BK_SSM_HOST=bkssm.service.consul \
  BK_IAM_PRIVATE_URL=$BK_IAM_PRIVATE_URL \
  BK_PAAS_FQDN=${BK_PAAS_FQDN:-${BK_PAAS_PUBLIC_ADDR%:*}} \
  BK_PAAS_HTTPS_PORT=${BK_PAAS_HTTPS_PORT:-443} \
  BK_PAAS_PRIVATE_URL=$BK_PAAS_PRIVATE_URL \
  BK_SSM_PORT=$BK_SSM_PORT \
  BK_LICENSE_PRIVATE_URL=$BK_LICENSE_PRIVATE_URL \
  BK_CODECC_PAAS_DIALOG_LOGIN_URL=$BK_PAAS_PUBLIC_URL/login/plain/?c_url= \
  BK_CODECC_PAAS_LOGIN_URL=\$BK_PAAS_PUBLIC_URL/login/\?c_url= \
  BK_CODECC_REPOSITORY_GITLAB_URL=http://\$BK_CODECC_FQDN \
  BK_CODECC_APP_CODE=bk_codecc \
  BK_CODECC_APP_TOKEN=$(uuid_v4) \
  BK_CODECC_GATEWAY_DNS_ADDR=127.0.0.1:53
# 复用rabbitmq, 生成密码并创建账户, 刷新03env.
set_env03 BK_CODECC_RABBITMQ_ADDR=$BK_RABBITMQ_IP:5672 BK_CODECC_RABBITMQ_USER=bk_codecc BK_CODECC_RABBITMQ_PASSWORD=$(random_pass) BK_CODECC_RABBITMQ_VHOST=bk_codecc
# 复用redis, 读取密码, 刷新03env.
set_env03 BK_CI_REDIS_HOST=$BK_REDIS_IP BK_CI_REDIS_PASSWORD=$BK_PAAS_REDIS_PASSWORD
# 这里确保和ci的redis保持一致.
set_env03 BK_CODECC_REDIS_HOST="$BK_CI_REDIS_HOST" BK_CODECC_REDIS_PASSWORD="$BK_CI_REDIS_PASSWORD"
# 复用mongodb, 读取密码, 刷新03env.
set_env03 BK_CODECC_MONGODB_ADDR="$BK_CMDB_MONGODB_HOST:$BK_CMDB_MONGODB_PORT" BK_CODECC_MONGODB_USER=bk_codecc BK_CODECC_MONGODB_PASSWORD="$(random_pass)"

if grep -w repo $CTRL_DIR/install.config|grep -v ^\# ; then
  set_env03 BK_REPO_GATEWAY_IP=$BK_REPO_GATEWAY_IP \
  BK_REPO_HOST=$BK_REPO_HOST
fi

echo "合并env."
./bin/merge_env.sh codecc &>/dev/null || true
