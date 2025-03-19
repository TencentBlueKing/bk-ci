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
BK_PKG_SRC_PATH=${BK_CI_SRC_DIR:-/data/src}
BK_CI_SRC_DIR="${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}"  # ci安装源
CTRL_DIR=${CTRL_DIR:-/data/install}
LAN_IP=${LAN_IP:-$(ip route show | grep -Pom 1 "(?<=src )[0-9.]+")}

BKCE_RENDER_CMD="$CTRL_DIR/bin/render_tpl"  # 蓝鲸社区版的render, 需要env文件及$BK_HOME.
CI_RENDER_CMD="$(dirname "$0")/render_tpl"  # bk-ci里的默认读取本地的bkenv.properties文件.
GEN_DOCKER_CONF_CMD="$(dirname "$0")/bk-ci-gen-docker-conf.sh"

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

sysctl_set (){
  SYSCTL_PATH="${SYSCTL_PATH:-/etc/sysctl.conf}"
  local kv k v effect_v
  for kv in "$@"; do
    k="${kv%%=*}"
    v="${kv#*=}"
    env_line_set "$SYSCTL_PATH" "$k " " $v"  # re-use env.
    sysctl -p >/dev/null  # 使之生效. 暂不使用--system
    effect_v=$(sysctl -n "$k")
    if test "$v" = "$effect_v"; then
      echo "sysctl_set: $k is set to $v."
    else
      echo "sysctl_set: failed set $k to $v."
      return 1
    fi
  done
}

# 负责渲染ci.
render_ci (){
  local proj=$1
  local files=()
  shopt -s nullglob
  case "$proj" in
    gateway)
      # 渲染可能存在的gateway配置文件.
      files+=("$BK_CI_SRC_DIR/support-files/templates/gateway#"*)
      ;&  # 这里不中断, 继续渲染frontend.
    frontend)
      # 渲染可能存在的frontend页面文件.
      files+=("$BK_CI_SRC_DIR/support-files/templates/frontend#"*)
      ;;
    *)
      # 渲染对应的微服务配置文件. 这里的模式必须通配到.
      files+=("$BK_CI_SRC_DIR/support-files/templates/#etc#ci#common.yml"
        "$BK_CI_SRC_DIR/support-files/templates/#etc#ci#"*"$proj."*
        "$BK_CI_SRC_DIR/support-files/templates/$proj"#*
        )
      ;;
  esac
  shopt -u nullglob
  if [ "${#files[@]}" -eq 0 ]; then
    echo "render_ci: no file matches, do nothing, proj is $proj."
    return 5
  fi
  if [ -x "$BKCE_RENDER_CMD" ]; then
    BK_ENV_FILE="$CTRL_DIR/bin/04-final/ci.env" $BKCE_RENDER_CMD -m ci -p "$BK_HOME" "${files[@]}"
  elif [ -x "$CI_RENDER_CMD" ]; then
    $CI_RENDER_CMD -m ci "${files[@]}"
  else
    echo >&2 "CI_RENDER_CMD is not executable: $CI_RENDER_CMD."
    return 1
  fi
}

## 配置ci
# 微服务公共配置.
setup_ci__ms_common (){
  #check_empty_var BK_CI_HOME BK_CI_LOGS_DIR BK_CI_DATA_DIR || return 15
  tip_dir_exist "$MS_DIR" || return 16
  [ -f "$service_env" ] || setup_ci__ms_service_env
  setup_ci__ms_start_env || return $?
  mkdir -p "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 1
  chown -R "$MS_USER:$MS_USER" "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 57
  chown "$MS_USER:$MS_USER" "$MS_DIR" || return 57
  # 创建符号链接logs及data指向对应目录.
  update_link_to_target "$MS_DIR/logs" "$MS_LOGS_DIR" || return 3
  update_link_to_target "$MS_DIR/data" "$MS_DATA_DIR" || return 3
  # 提供 MS_NAME.log 日志路径. 确保自定义tag时的日志路径统一.
  update_link_to_target "$MS_DIR/logs/$MS_NAME.log" "$MS_LOGS_DIR/$MS_NAME-$BK_CI_CONSUL_DISCOVERY_TAG.log" || return 3
  # 渲染微服务.
  render_ci "$MS_NAME" || return $?
}

# 微服务service.env
setup_ci__ms_service_env (){
  #check_empty_var BK_CI_HOME || return 15
  # 不用设置MAIN_CLASS. 这个应该由slim.sh设置. 仅新增, 不变动原有内容.
  env_line_append "$service_env" "MEM_OPTS" "-Xms256m -Xmx512m"
  env_line_append "$service_env" "CLASSPATH" ".:lib/*"
  env_line_append "$service_env" "SPRING_CONFIG_LOCATION" "file:./application.yml"
  env_line_append "$service_env" "SPRING_CLOUD_CONFIG_ENABLED" false
  env_line_append "$service_env" "JAVA_TOOL_OPTIONS" "-Dspring.main.allow-circular-references=true -Djava.security.egd=file:/dev/urandom -Dcertificate.file= -Dservice.log.dir=./logs/ -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=utf8 -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:+UseG1GC"
}

# 微服务启动env.
setup_ci__ms_start_env (){
  #check_empty_var BK_CI_HOME BK_CI_CONF_DIR || return 15
  local port_key=BK_CI_${MS_NAME_WORD^^}_API_PORT
  local conf_path="file://$MS_DIR/application.yml,file://$BK_CI_CONF_DIR/common.yml,file://$BK_CI_CONF_DIR/application-$MS_NAME.yml"
  env_line_set "$start_env" "DEVOPS_GATEWAY" "$BK_CI_HOST"
  env_line_set "$start_env" "SPRING_CONFIG_LOCATION" "$conf_path"
  env_line_set "$start_env" "MS_USER" "$MS_USER"
  env_line_set "$start_env" "DISCOVERY_TAG" "$BK_CI_CONSUL_DISCOVERY_TAG"
  env_line_set "$start_env" "API_PORT" "${!port_key:-}"
  #env_line_set "$start_env" ""
}
setup_ci__gw_start_env (){
  #check_empty_var BK_CI_HOME || return 15
  local start_env="$MS_DIR/start.env"
  env_line_set "$start_env" "MS_USER" "$MS_USER"
  chown "$MS_USER:$MS_USER" "$start_env" || return 5
}

# assembly.
setup_ci_assembly (){
  echo "setup_ci_assembly: assembly is not implemented yet."
  return 1
}

# artifactory, 需要检查是否为共享存储. 复制数据目录.
# 是否应该引入新的变量?
# BK_CI_ARTIFACTORY_NFS_FS_SPEC
# BK_CI_ARTIFACTORY_NFS_MOUNT_OPTIONS
setup_ci_artifactory (){
  local proj=$1
  setup_ci__ms_common "$proj" || return 11
  # 怎么发现多节点部署?
  # 检查服务发现地址? 如果存在且非自己, 则检查数据目录为共享文件系统?
  check_empty_var BK_CI_ARTIFACTORY_IP_COMMA || return 15
  # 判断节点数量.
  if [ "${BK_CI_ARTIFACTORY_IP_COMMA//,/}" != "${BK_CI_ARTIFACTORY_IP_COMMA}" ]; then
    echo >&2 "multiple ci-artifactory instances configured. data dir should be a shared filesystem."
    echo ""
  fi
  cp -nrv "$BK_CI_SRC_DIR/support-files/file" "$BK_CI_DATA_DIR/artifactory/"
  chown -R "$MS_USER:$MS_USER" "$BK_CI_DATA_DIR/artifactory/file" || true
}

setup_ci_auth (){
  local proj=$1
  setup_ci__ms_common "$proj" || return 11
  # 注册 ci-auth.service.consul, 供iam回调使用. 请勿更改此名称. 如不对接蓝鲸, 则可跳过ci-auth相关的操作.
  if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
    check_empty_var BK_CI_AUTH_API_PORT LAN_IP || return 15
    $CTRL_DIR/bin/reg_consul_svc -n ci-auth -p ${BK_CI_AUTH_API_PORT} -a $LAN_IP -D > /etc/consul.d/service/ci-auth.json 2>/dev/null || return 11
    consul reload
  else
    echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: ci-auth.service.consul."
  fi
}

# agentless 无实体服务, 为dockerhost加载不同配置的版本. 占位符已预设, 故复用安装逻辑.
setup_ci_agentless (){
  local proj=$1
  setup_ci_dockerhost agentless
  # agentless 复用dockerhost时, 其微服务日志的名称为dockerhost.
  update_link_to_target "$BK_CI_HOME/$proj/logs/$MS_NAME.log" "$BK_CI_HOME/$proj/logs/dockerhost-$BK_CI_CONSUL_DISCOVERY_TAG.log" || return 3
}
setup_ci_dockerhost (){
  local proj=$1
  setup_ci__ms_common "$proj" || return 11
  # 在当前目录创建docker及构建相关的链接. 方便排查问题.
  update_link_to_target "$BK_CI_HOME/$proj/build-logs" "$BK_CI_LOGS_DIR/docker" || return 3
  update_link_to_target "$BK_CI_HOME/$proj/build-data" "$BK_CI_DATA_DIR/docker" || return 3
  # 设置ipv4转发
  sysctl_set net.ipv4.ip_forward=1
  # 配置docker.
  ci_docker_data_root=$(readlink -f "$BK_CI_DATA_DIR/../docker-bkci")
  update_link_to_target "$BK_CI_HOME/$proj/docker-bkci" "$ci_docker_data_root" || return 3
  mkdir -p "$ci_docker_data_root" || return 2
  usermod -aG docker "$MS_USER" || return 1  # 加入用户组.
  $GEN_DOCKER_CONF_CMD "$ci_docker_data_root" || return $?  # 修改daemon.json.
  # 配置sigar路径.
  env_line_set "$start_env" "LD_LIBRARY_PATH" "$BK_CI_HOME/$proj/sigar/"
}

setup_ci_turbo (){
  local proj=$1
  setup_ci__ms_common "$proj" || return 11
  # turbo日志路径为 turbo-devops/turbo-devops.log
  update_link_to_target "$MS_DIR/logs/$MS_NAME.log" "$MS_LOGS_DIR/$MS_NAME-$BK_CI_CONSUL_DISCOVERY_TAG/$MS_NAME-$BK_CI_CONSUL_DISCOVERY_TAG.log" || return 3
  # 需要自定义启动参数.
  env_line_set "$start_env" "JAVA_OPTS" "-Dturbo.thirdparty.propdir=$BK_HOME/etc/ci/thirdparty"
  render_ci quartz  # 额外渲染 #etc#ci#thirdparty#quartz.properties
}

# 校验网关关键配置, 设置家目录, 设置启动用户或setcap?
setup_ci_gateway (){
  #check_empty_var BK_CI_DATA_DIR BK_CI_HOME BK_CI_LOGS_DIR || return 15
  setup_ci__gw_start_env || return $?
  # 判断nginx.conf路径
  local gateway_dir="$BK_CI_HOME/gateway" nginx_conf='' nginx_conf_dir=''
  if [ -f "$gateway_dir/core/nginx.conf" ]; then
    nginx_conf="$gateway_dir/core/nginx.conf"
    nginx_conf_dir="$gateway_dir/core"
  elif [ -f "$gateway_dir/nginx.conf" ]; then
    nginx_conf="$gateway_dir/nginx.conf"
    nginx_conf_dir="$gateway_dir"
  else
    echo >&2 "ERROR: unsupported ci-gateway dir."
    return 4
  fi
  # 更新conf目录的指向.
  update_link_to_target "$gateway_dir/conf" "$nginx_conf_dir" || return 3
  # 创建并更新logs目录.
  mkdir -p "$BK_CI_LOGS_DIR/nginx" || return 2
  chown -R "$MS_USER:$MS_USER" "$BK_CI_LOGS_DIR/nginx" || return 5
  update_link_to_target "$gateway_dir/logs" "$BK_CI_LOGS_DIR/nginx" || return 3
  update_link_to_target "$gateway_dir/run" "$BK_CI_LOGS_DIR/nginx" || return 3
  # 在数据目录创建运行时的存储目录, 并更新链接.
  local gateway_data_dir="$BK_CI_DATA_DIR/gateway" temp_dir=
  mkdir -p "$gateway_data_dir" || return 1
  for temp_dir in client_body_temp fastcgi_temp proxy_temp scgi_temp uwsgi_temp files; do
    mkdir -p "$gateway_data_dir/$temp_dir" || return 2
    chown "$MS_USER:$MS_USER" "$gateway_data_dir/$temp_dir" || return 5
    update_link_to_target "$gateway_dir/$temp_dir" "$gateway_data_dir/$temp_dir" || return 3
  done
  # 检查 bk_login_v3 的lua文件, 如果没有, 就使用v2的替代.
  if [ "${BK_CI_AUTH_PROVIDER:-sample}" = "bk_login_v3" ]; then
    if ! cp -nv "$nginx_conf_dir/lua/auth/auth_user-bk_login.lua" \
                "$nginx_conf_dir/lua/auth/auth_user-bk_login_v3.lua"; then
      echo >&2 "failed to prepare auth_user-bk_login_v3.lua"
      return 6
    fi
  fi
  # prod目录指向agent-package.
  # 预期200: curl -I bk-ci.service.consul/static/files/jar/worker-agent.jar
  update_link_to_target "$gateway_data_dir/files/prod" "$BK_CI_HOME/agent-package"
  cp -f "$gateway_data_dir/files/prod/script/docker_init.sh" "$gateway_data_dir/files/prod/"
  # 在全部 ci-gateway 节点上注册主入口域名: bk-ci.service.consul, 用于在集群内提供web服务.
  if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
    check_empty_var LAN_IP || return 15
    $CTRL_DIR/bin/reg_consul_svc -n bk-ci -p ${BK_CI_HTTP_PORT:-80} -a "$LAN_IP" -D > /etc/consul.d/service/bk-ci.json 2>/dev/null || return 11
    consul reload
  else
    echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: bk-ci.service.consul."
  fi
  if ! grep -w repo $CTRL_DIR/install.config|grep -v ^\# ; then
    > $BK_CI_SRC_DIR/support-files/templates/gateway\#core\#vhosts\#devops.bkrepo.upstream.conf
  else
    cat > $BK_CI_SRC_DIR/support-files/templates/gateway\#core\#vhosts\#devops.bkrepo.upstream.conf << EOF 
upstream __BK_REPO_HOST__ {
    server __BK_REPO_GATEWAY_IP__;
}
EOF
  fi
  # 渲染gateway配置及frontend页面.
  render_ci "$MS_NAME" || return $?
}

MS_NAME=$1
shift
# 检查环境变量
check_empty_var BK_CI_HOME BK_CI_DATA_DIR BK_CI_LOGS_DIR BK_CI_CONSUL_DISCOVERY_TAG
# 本脚本设计为快速滚动更新. 故不会主动新增部署, 仅处理已有的proj.
# 判断服务是否启用.
is_sd_service_enabled (){
  systemctl is-enabled "bk-ci-$1" &>/dev/null
}
# 判断是否启用了服务, 如果未启用, 则提示显示启用的方法.
if ! is_sd_service_enabled "$MS_NAME"; then
  echo "NOTE: service $MS_NAME not enabled in this node. run bk-ci-reg-systemd.sh to enable it."
  exit 0
fi
# 公共变量
MS_NAME_WORD=${MS_NAME//-/_}
MS_DIR="$BK_CI_HOME/$MS_NAME"
MS_LOGS_DIR="$BK_CI_LOGS_DIR/$MS_NAME"
MS_DATA_DIR="$BK_CI_DATA_DIR/$MS_NAME"
BK_HOME="${BK_HOME:-$(readlink -f "$BK_CI_HOME/..")}"
BK_CI_CONF_DIR="${BK_CI_CONF_DIR:-$BK_HOME/etc/ci}"
service_env="$MS_DIR/service.env"
start_env="$MS_DIR/start.env"
setup_func=setup_ci_$MS_NAME_WORD
if declare -f "$setup_func" >/dev/null; then
  $setup_func "$MS_NAME" "$@"
else
  echo "INFO: $setup_func not defined, try to call default handler.."
  setup_ci__ms_common "$MS_NAME" "$@"
fi


