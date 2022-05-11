#!/bin/bash
# 在蓝鲸社区版中部署时, 检查install.config内容.

set -eu
trap "on_ERR;" ERR
on_ERR (){
    local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
    echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

my_path="$(readlink -f "$0")"

show_repo_installconfig_example (){
    echo " 参考示例：（请修改IP1等为合适的IP）"
    # 列出全部微服务, 大概5个1行.
cat <<EOF
    # 服务端(微服务), 单节点要求最低配置4核8G. 后期可升级节点硬件配置或分散微服务到不同节点.
    IP1 repo(gateway)  # repo-gateway为配置文件, 需要放在paas nginx一起.
    IP2 repo(auth),repo(generic),repo(repository)
EOF
}
set -a
CTRL_DIR="${CTRL_DIR:-/data/install}"
echo "加载 $CTRL_DIR/load_env.sh."
if [ -r "$CTRL_DIR/load_env.sh" ]; then
    source $CTRL_DIR/load_env.sh
else
    echo "请先安装蓝鲸社区版, 在中控机修改install.config, 然后执行本脚本."
    show_repo_installconfig_example
    exit 3
fi
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_REPO_SRC_DIR=${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}
set +a
repo_env_03="$CTRL_DIR/bin/03-userdef/repo.env"

# 判断环境变量, 提示先填写 install.config.
if [ -z "${BK_REPO_IP_COMMA:-}" ]; then
    echo " 请先更新 $CTRL_DIR/install.config 文件，新增 REPO 的分布规则。"
    show_repo_installconfig_example
    exit 1
else
    echo " 发现 REPO 节点: $BK_REPO_IP_COMMA"
fi
# 同主机相斥: ip_commas modules reason suggestion
install_config_conflict (){
    local conflict_ip=$(echo "$1" | tr ',' '\n' | sort | uniq -d)
    if [ -n "$conflict_ip" ]; then
        echo "install.config中$2模块部署到了相同的主机: ${conflict_ip//$'\n'/,}, 因为二者均${3:-未填写原因}, 建议${4:-无}."
        return 1
    fi
}
# 同主机相吸: ip_commas modules
install_config_affinity (){
    local lonely_ip=$(echo "$1" | tr ',' '\n' | sort | uniq -u)
    if [ -n "$lonely_ip" ]; then
        echo "install.config中$2模块需要部署到相同的主机, 但是如下IP中仅配置了其中一项: ${lonely_ip//$'\n'/,}."
        return 1
    fi
}
# 需要存在. ip_comma module tip
install_config_exist (){
    if [ -n "$1" ]; then
        echo "install.config中存在$2."
    else
        echo "install.config中未定义$2. ${3:-}"
        return 1
    fi
}

service_up (){
    if getent hosts "$1"; then
        echo "服务 $2 存在."
    else
        echo "服务 $2 未安装或未启动."
        return 1
    fi
}

source $CTRL_DIR/bin/02-dynamic/hosts.env
echo "检查 install.config, 请根据提示处理."
# 蓝鲸依赖.
install_config_exist "${BK_MYSQL_IP_COMMA:-}" \
    "mysql" \
    "新增 REPO 配置项时请勿删除install.config原有内容."

install_config_exist "${BK_MONGODB_IP_COMMA:-}" \
    "mongodb" \
    "新增 REPO 配置项时请勿删除install.config原有内容."

# repo组件配置.
install_config_exist "${BK_REPO_GATEWAY_IP:-}" \
    "repo(gateway)" \
    "必须配置repo(gateway)."
install_config_exist "${BK_REPO_AUTH_IP_COMMA:-}" \
    "repo(auth)" \
    "必须配置repo(auth)."
install_config_exist "${BK_REPO_GENERIC_IP_COMMA:-}" \
    "repo(generic)" \
    "必须配置repo(generic)."
install_config_exist "${BK_REPO_REPOSITORY_IP_COMMA:-}" \
    "repo(repository)" \
    "必须配置repo(repository)."

echo "检查 install.config 通过."

