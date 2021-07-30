#!/bin/bash
# 在蓝鲸社区版中部署时, 检查install.config内容.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

my_path="$(readlink -f "$0")"
cmd_collect_codecc_ms_name="${my_path%/*}/bk-codecc-collect-ms-name.sh"

show_codecc_installconfig_example (){
  echo " 参考示例：（请修改IP1等为合适的IP）"
  # 列出全部微服务, 大概5个1行.
  cat <<EOF
# 服务端(网关+微服务), 单节点要求最低配置4核8G. 后期可升级节点硬件配置或分散微服务到不同节点.
# 需要将codecc(gateway)和ci(gateway)分开, 二者均为独占80端口.
IP1 codecc(gateway)$($cmd_collect_codecc_ms_name | awk -v RS="( |\n)+" '{name="codecc("$1")"; if(++n%5==1){ printf "\nIP1 "; comma=""}; printf comma name; comma=","}')
EOF
}
set -a
CTRL_DIR="${CTRL_DIR:-/data/install}"
echo "加载 $CTRL_DIR/load_env.sh."
if [ -r "$CTRL_DIR/load_env.sh" ]; then
  source $CTRL_DIR/load_env.sh
else
  echo "请先安装蓝鲸社区版, 在中控机修改install.config, 然后执行本脚本."
  show_codecc_installconfig_example
  exit 3
fi
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CODECC_SRC_DIR=${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}
set +a
codecc_env_03="$CTRL_DIR/bin/03-userdef/codecc.env"

# 判断环境变量, 提示先填写 install.config.
if [ -z "${BK_CODECC_IP_COMMA:-}" ]; then
  echo " 请先更新 $CTRL_DIR/install.config 文件，新增 CODECC 的分布规则。"
  show_codecc_installconfig_example
  exit 1
else
  echo " 发现 CODECC 节点: $BK_CODECC_IP_COMMA"
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

# 在线服务检查.
if service_up paas.service.consul "paas" &&
   service_up bkiam.service.consul "iam" &&
   service_up bkssm.service.consul "ssm"
then
  echo "依赖的蓝鲸服务均已启动."
else
  echo "请先排查上述检查不通过的蓝鲸服务, 如果服务正常, 请检查consul是否正常."
  exit 1
fi

echo "检查 install.config, 请根据提示处理."
# 蓝鲸冲突检测
install_config_conflict "${BK_NGINX_IP_COMMA:-},${BK_CODECC_GATEWAY_IP_COMMA:-}" \
  "codecc(gateway)与nginx" "需要独占80端口" \
  "将codecc(gateway)移到其他节点"

# 蓝鲸依赖.
install_config_exist "${BK_CONSUL_IP_COMMA:-}" \
  "consul" \
  "新增 CODECC 配置项时请勿删除install.config原有内容."

# 可选依赖识别
install_config_exist "${BK_CODECC_RABBITMQ_ADDR:-${BK_RABBITMQ_IP:-}}" \
  "rabbitmq" \
  "或在 $codecc_env_03 中定义 BK_CODECC_RABBITMQ_ADDR"
install_config_exist "${BK_CODECC_ES_REST_ADDR:-${BK_ES7_IP:-}}" \
  "es7" \
  "或在 $codecc_env_03 中定义 BK_CODECC_ES_REST_ADDR"
install_config_exist "${BK_CODECC_REDIS_HOST:-${BK_REDIS_IP:-}}" \
  "redis" \
  "或在 $codecc_env_03 中定义 BK_CODECC_REDIS_HOST"
install_config_exist "${BK_CODECC_MONGODB_ADDR:-${BK_MONGODB_IP:-}}" \
  "mongodb" \
  "或在 $codecc_env_03 中定义 BK_CODECC_MONGODB_ADDR"

# codecc旧组件名称提示
if install_config_exist "${BK_CODECC_WEB_IP:-}" "codecc(web)" &>/dev/null
then
  echo "install.config中存在旧配置codecc(web) , 请参考如下模板重新编写新版."
  show_codecc_installconfig_example
  exit 1
fi

# codecc组件配置.
install_config_exist "${BK_CODECC_GATEWAY_IP:-}" \
  "codecc(gateway)" \
  "必须配置CODECC网关."

# codecc(gateway)应和ci(gateway)在不同主机.
install_config_conflict "${BK_CI_GATEWAY_IP:-},${BK_CODECC_GATEWAY_IP:-}" \
  "ci(gateway) 和 codecc(gateway)" "需要独占80端口" \
  "将codecc(gateway)移到其他节点"

echo "检查 install.config 通过."
