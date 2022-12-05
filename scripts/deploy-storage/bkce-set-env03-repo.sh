#!/bin/bash
# 在蓝鲸社区版中, 生成所需的repo.env文件.
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
BK_REPO_SRC_DIR=${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}
BK_REPO_HTTP_HEAD=$(echo $BK_PAAS_FQDN|awk -F\. '{print $1}'|sed "s#paas#repo#g")
BK_CI_HTTP_HEAD=$(echo $BK_PAAS_FQDN|awk -F\. '{print $1}'|sed "s#paas#devops#g")
set +a
repo_env_default="$CTRL_DIR/bin/default/repo.env"
repo_env_03="$CTRL_DIR/bin/03-userdef/repo.env"
repo_env_04="$CTRL_DIR/bin/04-final/repo.env"

ci_env_default="$CTRL_DIR/bin/default/ci.env"
ci_env_03="$CTRL_DIR/bin/03-userdef/ci.env"
ci_env_04="$CTRL_DIR/bin/04-final/ci.env"

set_env03 (){
    for kv in "$@"; do
        if ! grep -q "^${kv%%=*}=[^ ]" "$repo_env_03" 2>/dev/null; then  # 非空则不覆盖.
            echo "SET_ENV03: $repo_env_03 中未曾赋值，新增 $kv"
            [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确，或人工修改env文件后重试。"
            # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
            grep -qxF "$kv" "$repo_env_03" 2>/dev/null || echo "$kv" >> "$repo_env_03"
            eval "$kv"  # 立即生效
        fi
    done
}

set_ci_env03 (){
    for kv in "$@"; do
        if ! grep -q "^${kv%%=*}=[^ ]" "$ci_env_03" 2>/dev/null; then  # 非空则不覆盖.
            echo "SET_ENV03: $ci_env_03 中未曾赋值，新增 $kv"
            [[ "$kv" =~ ^[A-Z0-9_]+=$ ]] && echo -e "\033[31;1m注意：\033[m$kv 赋值为空，请检查蓝鲸是否安装正确，或人工修改env文件后重试。"
            # 如果已经有相同的行，则也不覆盖，防止赋值为空时不断追加。
            grep -qxF "$kv" "$ci_env_03" 2>/dev/null || echo "$kv" >> "$ci_env_03"
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
# 批量检查变量名为空的情况.
check_empty_var (){
    local k='' e=0
    for k in "$@"; do
        if [ -z "${!k:-}" ]; then
            echo >&2 "var $k is empty or not set."
            ((++e))
        fi
    done
    return "$e"
}

cd "$CTRL_DIR"
pkg_env_tpl="$BK_PKG_SRC_PATH/repo/scripts/repo.env"
# 提前处理
sed -i "/BK_CI_AUTH_TOKEN/d;/BK_CI_BKREPO_AUTHORIZATION/d;/BK_REPO_MONGODB_URI/d" $pkg_env_tpl;sed -i "/BK_REPO_MONGODB_URI/d" $repo_env_default;sed -i "/BK_CI_AUTH_TOKEN/d;/BK_REPO_MONGODB_URI/d" $repo_env_03;sed -i "/BK_CI_AUTH_TOKEN/d" $repo_env_04;sed -i "/BK_CI_AUTH_TOKEN/d" $ci_env_03
cat >> $pkg_env_tpl << EOF
BK_CI_BKREPO_AUTHORIZATION=
BK_CI_AUTH_TOKEN=
BK_REPO_MONGODB_URI=
EOF
if [[ -z ${BK_REPO_MONGODB_PASSWORD} ]];then BK_REPO_MONGODB_PASSWORD="$(random_pass)";else BK_REPO_MONGODB_PASSWORD=$BK_REPO_MONGODB_PASSWORD;fi

set -vx
if [ -f "$pkg_env_tpl" ] && ! diff -q "$pkg_env_tpl" "$repo_env_default" >/dev/null; then
    echo "安装包中存在新版env文件, 更新repo.env模板: $repo_env_default"
    cp -v "$pkg_env_tpl" "$repo_env_default" || echo "更新repo.env模板失败."
else
    echo "无需更新 $repo_env_default."
fi
set +vx
echo "---------------------------------------------------"
 
set +x
echo "检查设置 REPO 基础配置"
set_env03 \
    BK_REPO_SRC_DIR=$BK_PKG_SRC_PATH/repo \
    BK_HTTP_SCHEMA=http \
    BK_REPO_APP_SECRET=$(uuid_v4) \
    BK_DOMAIN=$BK_DOMAIN \
    BK_HOME=$BK_HOME \
    BK_REPO_FQDN=repo.\$BK_DOMAIN \
    BK_REPO_PUBLIC_URL=http://\$BK_REPO_FQDN \
    BK_REPO_APP_CODE=bk_repo \
    BK_REPO_APP_TOKEN=$(uuid_v4) \
    BK_PAAS_PUBLIC_URL=$BK_PAAS_PUBLIC_URL \
    BK_REPO_AUTH_PROVIDER=bk_login_v3 \
    BK_SSM_HOST=bkssm.service.consul \
    BK_IAM_PRIVATE_URL=$BK_IAM_PRIVATE_URL \
    BK_PAAS_FQDN=${BK_PAAS_FQDN:-${BK_PAAS_PUBLIC_ADDR%:*}} \
    BK_PAAS_HTTPS_PORT=${BK_PAAS_HTTPS_PORT:-443} \
    BK_PAAS_PRIVATE_URL=$BK_PAAS_PRIVATE_URL \
    BK_REPO_SSM_ENV=prod \
    BK_REPO_SSM_HOST=$BK_SSM_HOST \
    BK_SSM_PORT=$BK_SSM_PORT \
    BK_REPO_SSM_HTTP_PORT=$BK_SSM_PORT \
    BK_REPO_SSM_IP0=$BK_SSM_HOST \
    BK_REPO_SSM_TOKEN_URL="/api/v1/auth/access-tokens" \
    BK_REPO_IAM_TOKEN_URL="/api/v1/auth/access-tokens" \
    BK_LICENSE_PRIVATE_URL=$BK_LICENSE_PRIVATE_URL \
    BK_REPO_PAAS_DIALOG_LOGIN_URL=$BK_PAAS_PUBLIC_URL/login/plain/?c_url= \
    BK_REPO_PAAS_LOGIN_URL=\$BK_PAAS_PUBLIC_URL/login/\?c_url= \
    BK_REPO_REPOSITORY_GITLAB_URL=http://\$BK_REPO_FQDN \
    BK_REPO_HOME=${BK_HOME:-/data/bkce}/repo \
    BK_REPO_LOGS_DIR=${BK_HOME:-/data/bkce}/logs/repo \
    BK_REPO_DATA_DIR=${BK_HOME:-/data/bkce}/public/repo \
    BK_REPO_CONSUL_DISCOVERY_TAG=devops \
    BK_REPO_REDIS_HOST=redis.service.consul \
    BK_REPO_REDIS_PORT=6379 \
    BK_REPO_REDIS_ADMIN_PASSWORD=$BK_REDIS_ADMIN_PASSWORD \
    BK_REPO_CONSUL_DOMAIN=consul \
    BK_REPO_CONSUL_TAG=dc \
    BK_REPO_DEPLOY_MODE=ci \
    BK_REPO_DOCKER_HTTP_PORT=80 \
    BK_REPO_HELM_HTTP_PORT=80 \
    BK_REPO_HTTP_PORT=80 \
    BK_REPO_MONGODB_USER=bk_repo \
    BK_REPO_HOST=$BK_REPO_HTTP_HEAD\.\$BK_DOMAIN \
    BK_REPO_API_URL="http://$BK_REPO_HTTP_HEAD.$BK_DOMAIN" \
    BK_CI_PUBLIC_URL="http://$BK_CI_HTTP_HEAD.$BK_DOMAIN" \
    BK_CI_ARTIFACTORY_REALM=bkrepo \
    BK_REPO_GENERIC_PORT=25801 \
    BK_REPO_OCI_HTTP_PORT=80 \
    BK_REPO_AUTH_REALM=devops \
    BK_CI_BKREPO_AUTHORIZATION="\"Platform MThiNjFjOWMtOTAxYi00ZWEzLTg5YzMtMWY3NGJlOTQ0YjY2OlVzOFpHRFhQcWs4NmN3TXVrWUFCUXFDWkxBa00zSw==\"" \
    BK_CI_AUTH_TOKEN=$BK_CI_APP_TOKEN \
    BK_REPO_MONGODB_PASSWORD=$BK_REPO_MONGODB_PASSWORD 
# 如下行原样注入。
set_env03 \
    BK_REPO_MONGODB_URI="mongodb://$BK_REPO_MONGODB_USER:$BK_REPO_MONGODB_PASSWORD@$BK_MONGODB_IP/bkrepo"
# 复用mongodb, 读取密码, 刷新03.env.
set_env03 \
    BK_REPO_MONGODB_ADDR="$BK_CMDB_MONGODB_HOST:$BK_CMDB_MONGODB_PORT" BK_REPO_MONGODB_USER=bk_repo BK_REPO_MONGODB_PASSWORD="$BK_REPO_MONGODB_PASSWORD"

set_ci_env03 \
    BK_REPO_HOST=$BK_REPO_HTTP_HEAD\.\$BK_DOMAIN \
    BK_REPO_API_URL="http://$BK_REPO_HTTP_HEAD.$BK_DOMAIN" \
    BK_CI_PUBLIC_URL="http://$BK_CI_HTTP_HEAD.$BK_DOMAIN" \
    BK_CI_ARTIFACTORY_REALM=bkrepo \
    BK_CI_BKREPO_AUTHORIZATION="\"Platform MThiNjFjOWMtOTAxYi00ZWEzLTg5YzMtMWY3NGJlOTQ0YjY2OlVzOFpHRFhQcWs4NmN3TXVrWUFCUXFDWkxBa00zSw==\"" \
    BK_CI_AUTH_TOKEN=$BK_CI_APP_TOKEN

set -x
echo "合并env."
$CTRL_DIR/bin/merge_env.sh ci &>/dev/null || true
$CTRL_DIR/bin/merge_env.sh repo &>/dev/null || true
