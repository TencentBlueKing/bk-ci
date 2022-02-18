#!/bin/bash
# shellcheck disable=SC2128
# 启动slim版的微服务的部署.
# 仅当存在微服务名时才执行部署. 否则返回0.

set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

if [ "${BASH_VERSINFO[0]:-0}" -lt 4 ]; then
  echo >&2 "this script need bash v4.x to run."
  exit 1
fi

if [ $# -lt 1 ]; then
  echo "Usage: $0 MS_NAME [OPTIONS-for-MS_NAME]"
  exit 1
fi
MS_USER=${MS_USER:-blueking}  # 暂不建议修改为其他用户, 此功能未测试.
BK_PKG_SRC_PATH=${BK_CODECC_SRC_DIR:-/data/src}
BK_CODECC_SRC_DIR="${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}"  # codecc安装源
CTRL_DIR=${CTRL_DIR:-/data/install}
LAN_IP=${LAN_IP:-$(ip route show | grep -Pom 1 "(?<=src )[0-9.]+")}

BKCE_RENDER_CMD="$CTRL_DIR/bin/render_tpl"  # 蓝鲸社区版的render, 需要env文件及$BK_HOME.
CODECC_RENDER_CMD="$(dirname "$0")/render_tpl"  # bk-codecc里的默认读取本地的bkenv.properties文件.
GEN_DOCKER_CONF_CMD="$(dirname "$0a")/bk-codecc-gen-docker-conf.sh"

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
tip_file_exist (){
  local m="file exist" e=0
  [ -f "$1" ] || { m="file not exist"; e=1; }
  echo "$1: $m."
  return "$e"
}
tip_dir_exist (){
  local m="dir exist" e=0
  [ -d "$1" ] || { m="dir not exist"; e=1; }
  echo "$1: $m."
  return "$e"
}
# 修正链接, 用于确保给定的链接符合预期.
# param 1: linkpath, create if not exist, fail if not a symlink.
# param 2: expected target
update_link_to_target (){
  local linkpath="$1"
  local target="$2"
  if [ -z "$linkpath" ] || [ -z "$target" ]; then
    echo >&2 "Usage: $FUNCNAME linkpath target  -- update link to target.";
    return 3;
  fi
  if [ -e "$linkpath" ] && ! [ -L "$linkpath" ]; then
    echo >&2 "$FUNCNAME: linkpath($linkpath) exist and not a link.";
    return 2;
  fi
  echo "$FUNCNAME: linkpath=$linkpath target=$target."
  ln -rsfT "$target" "$linkpath" || { echo >&2 "$FUNCNAME: fail when update link $linkpath."; return 1; }
}

# 更新env文件. 单行.
env_line_update (){
  local f="$1" k="$2" v="$3"
  local sep=$'\1'
  sed -i -e "s$sep^$k=.*$sep$k=${v//$sep/}$sep" "$f"
}
env_line_append (){  # append应检查.
  local f="$1" k="$2" v="$3"
  if grep -q "^$k=" "$f" 2>/dev/null; then
    return 52  # 已经存在, 返回52.
  else
    echo "$k=$v" >> "$f"
  fi
}
env_line_set (){
  env_line_append "$@" || env_line_update "$@"
}

# 负责渲染codecc.
render_codecc (){
  local proj=$1
  local files=()
  shopt -s nullglob
  case "$proj" in
    gateway)
      # 渲染可能存在的gateway配置文件.
      files+=("$BK_CODECC_SRC_DIR/support-files/templates/gateway#"*)
      ;&  # 这里不中断, 继续渲染frontend.
    frontend)
      # 渲染可能存在的frontend页面文件.
      files+=("$BK_CODECC_SRC_DIR/support-files/templates/frontend#"*)
      ;;
    *)
      # 渲染对应的微服务配置文件. 这里的模式必须通配到.
      files+=("$BK_CODECC_SRC_DIR/support-files/templates/#etc#codecc#common.yml"
        "$BK_CODECC_SRC_DIR/support-files/templates/#etc#codecc#"*"$proj."*
        "$BK_CODECC_SRC_DIR/support-files/templates/$proj"#*
        )
      ;;
  esac
  shopt -u nullglob
  if [ "${#files[@]}" -eq 0 ]; then
    echo "render_codecc: no file matches, do nothing, proj is $proj."
    return 0
  fi
  if [ -x "$BKCE_RENDER_CMD" ]; then
    BK_ENV_FILE="$CTRL_DIR/bin/04-final/codecc.env" $BKCE_RENDER_CMD -u -m codecc -p "$BK_HOME" "${files[@]}"
  elif [ -x "$CODECC_RENDER_CMD" ]; then
    $CODECC_RENDER_CMD -m codecc "${files[@]}"
  else
    echo >&2 "CODECC_RENDER_CMD is not executable: $CODECC_RENDER_CMD."
    return 1
  fi
}

## 配置codecc
# 微服务公共配置.
setup_codecc__ms_common (){
  #check_empty_var BK_CODECC_HOME BK_CODECC_LOGS_DIR BK_CODECC_DATA_DIR || return 15
  tip_dir_exist "$MS_DIR" || return 16
  [ -f "$service_env" ] || setup_codecc__ms_service_env
  setup_codecc__ms_start_env || return $?
  mkdir -p "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 1
  chown -R "$MS_USER:$MS_USER" "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 57
  chown "$MS_USER:$MS_USER" "$MS_DIR" || return 57
  # 创建符号链接logs及data指向对应目录.
  update_link_to_target "$MS_DIR/logs" "$MS_LOGS_DIR" || return 3
  update_link_to_target "$MS_DIR/data" "$MS_DATA_DIR" || return 3
  # 提供 MS_NAME.log 日志路径. 确保自定义tag时的日志路径统一.
  update_link_to_target "$MS_DIR/logs/$MS_NAME.log" "$MS_LOGS_DIR/$MS_NAME-$BK_CODECC_CONSUL_DISCOVERY_TAG.log" || return 3
  # 渲染微服务.
  render_codecc "$MS_NAME" || return $?
}

# 微服务service.env
setup_codecc__ms_service_env (){
  #check_empty_var BK_CODECC_HOME || return 15
  # 不用设置MAIN_CLASS. 这个应该由slim.sh设置. 仅新增, 不变动原有内容.
  env_line_append "$service_env" "MEM_OPTS" "-Xms256m -Xmx512m"
  env_line_append "$service_env" "CLASSPATH" ".:lib/*"
  env_line_append "$service_env" "SPRING_CONFIG_LOCATION" "file:./application.yml"
  env_line_append "$service_env" "SPRING_CLOUD_CONFIG_ENABLED" false
  env_line_append "$service_env" "JAVA_TOOL_OPTIONS" "-Djava.security.egd=file:/dev/urandom -Dcertificate.file= -Dservice.log.dir=./logs/ -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=utf8 -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:+UseConcMarkSweepGC"
}

# 微服务启动env.
setup_codecc__ms_start_env (){
  #check_empty_var BK_CODECC_HOME BK_CODECC_CONF_DIR || return 15
  local port_key=BK_CODECC_${MS_NAME_WORD^^}_API_PORT
  local conf_path="file://$MS_DIR/application.yml,file://$BK_CODECC_CONF_DIR/common.yml,file://$BK_CODECC_CONF_DIR/application-$MS_NAME.yml"
  env_line_set "$start_env" "DEVOPS_GATEWAY" "$BK_CODECC_HOST"
  env_line_set "$start_env" "SPRING_CONFIG_LOCATION" "$conf_path"
  env_line_set "$start_env" "MS_USER" "$MS_USER"
  env_line_set "$start_env" "DISCOVERY_TAG" "$BK_CODECC_CONSUL_DISCOVERY_TAG"
  env_line_set "$start_env" "API_PORT" "${!port_key:-}"
  #env_line_set "$start_env" ""
}
setup_codecc__gw_start_env (){
  #check_empty_var BK_CI_HOME || return 15
  local start_env="$MS_DIR/start.env"
  env_line_set "$start_env" "MS_USER" "$MS_USER"
  chown "$MS_USER:$MS_USER" "$start_env" || return 5
}

# assembly.
setup_codecc_assembly (){
  echo "setup_codecc_assembly: assembly is not implemented yet."
  return 1
}

# schedule 需要额外的数据文件.
setup_codecc_schedule (){
  local proj=$1
  setup_codecc__ms_common "$proj" || return 11
  check_empty_var BK_CODECC_FILE_DATA_PATH MS_USER || return 15
  # 判断节点数量.
  if [ "${BK_CODECC_SCHEDULE_IP_COMMA//,/}" != "${BK_CODECC_SCHEDULE_IP_COMMA}" ]; then
    echo >&2 "multiple codecc-schedule instances configured. dir should be a shared filesystem: $script_download_dir."
    echo ""
  fi
  # 刷新script_download数据.
  local script_download_dir="$BK_CODECC_FILE_DATA_PATH/download/script_download/"
  mkdir -p "$script_download_dir"
  chown "$MS_USER:$MS_USER" "$BK_CODECC_FILE_DATA_PATH"
  rsync -rav "$BK_CODECC_SRC_DIR/support-files/script_download/" "$script_download_dir"
  chown "$MS_USER:$MS_USER" "$BK_CODECC_FILE_DATA_PATH/download" -R
}

# CodeCC网关即为CI网关, 这里创建符号链接指向ci网关.
setup_codecc_gateway (){
  check_empty_var BK_CI_HOME BK_CODECC_DATA_DIR BK_CODECC_HOME BK_CODECC_LOGS_DIR || return 15
  setup_codecc__gw_start_env || return $?
  # 判断nginx.conf路径
  local gateway_dir="$BK_CODECC_HOME/gateway" nginx_conf='' nginx_conf_dir=''
  if [ -f "$gateway_dir/core/nginx.conf" ]; then
    nginx_conf="$gateway_dir/core/nginx.conf"
    nginx_conf_dir="$gateway_dir/core"
  else
    echo >&2 "ERROR: unsupported ci-gateway dir."
    return 4
  fi
  # 更新conf目录的指向.
  update_link_to_target "$gateway_dir/conf" "$nginx_conf_dir" || return 3
  # 创建并更新logs目录.
  mkdir -p "$BK_CODECC_LOGS_DIR/nginx" || return 2
  chown -R "$MS_USER:$MS_USER" "$BK_CODECC_LOGS_DIR/nginx" || return 5
  update_link_to_target "$gateway_dir/logs" "$BK_CODECC_LOGS_DIR/nginx" || return 3
  update_link_to_target "$gateway_dir/run" "$BK_CODECC_LOGS_DIR/nginx" || return 3
  # 在数据目录创建运行时的存储目录, 并更新链接.
  local gateway_data_dir="$BK_CODECC_DATA_DIR/gateway" temp_dir=
  mkdir -p "$gateway_data_dir" || return 1
  for temp_dir in client_body_temp fastcgi_temp proxy_temp scgi_temp uwsgi_temp files; do
    mkdir -p "$gateway_data_dir/$temp_dir" || return 2
    chown "$MS_USER:$MS_USER" "$gateway_data_dir/$temp_dir" || return 5
    update_link_to_target "$gateway_dir/$temp_dir" "$gateway_data_dir/$temp_dir" || return 3
  done
  # 尝试规避codecc复用ci-gateway时devops.server.conf不存在的问题.
  if grep -Fwq "devops.server.conf" "$gateway_dir/core/nginx.conf"; then
    touch "$gateway_dir/core/devops.server.conf"
  fi
  # 在全部 codecc-gateway 节点上注册主入口域名: bk-codecc.service.consul, 用于在集群内提供web服务.
  if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
    check_empty_var LAN_IP || return 15
    $CTRL_DIR/bin/reg_consul_svc -n bk-codecc -p ${BK_CODECC_HTTP_PORT:-80} -a "$LAN_IP" -D > /etc/consul.d/service/bk-ci.json 2>/dev/null || return 11
    consul reload
  else
    echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: bk-ci.service.consul."
  fi
  # 渲染gateway配置及frontend页面.
  render_codecc "$MS_NAME" || return $?
  # TODO 检查nginx可读取到codecc文件.
}

MS_NAME=$1
shift
# 检查环境变量
check_empty_var BK_CODECC_HOME BK_CODECC_DATA_DIR BK_CODECC_LOGS_DIR BK_CODECC_CONSUL_DISCOVERY_TAG
# 本脚本设计为快速滚动更新. 故不会主动新增部署, 仅处理已有的proj.
# 判断服务是否启用.
is_sd_service_enabled (){
  systemctl is-enabled "bk-codecc-$1" &>/dev/null
}
# 判断是否启用了服务, 如果未启用, 则提示显示启用的方法.
if ! is_sd_service_enabled "$MS_NAME"; then
  echo "NOTE: service $MS_NAME not enabled in this node. run bk-codecc-reg-systemd.sh to enable it."
  exit 0
fi
# 公共变量
MS_NAME_WORD=${MS_NAME//-/_}
MS_DIR="$BK_CODECC_HOME/$MS_NAME"
MS_LOGS_DIR="$BK_CODECC_LOGS_DIR/$MS_NAME"
MS_DATA_DIR="$BK_CODECC_DATA_DIR/$MS_NAME"
BK_HOME="${BK_HOME:-$(readlink -f "$BK_CODECC_HOME/..")}"
BK_CODECC_CONF_DIR="${BK_CODECC_CONF_DIR:-$BK_HOME/etc/codecc}"
service_env="$MS_DIR/service.env"
start_env="$MS_DIR/start.env"
setup_func=setup_codecc_$MS_NAME_WORD
if declare -f "$setup_func" >/dev/null; then
  $setup_func "$MS_NAME" "$@"
else
  echo "INFO: $setup_func not defined, try to call default handler.."
  setup_codecc__ms_common "$MS_NAME" "$@"
fi


