#!/bin/bash
# 启动slim版的微服务.
# 兼职启动gateway.
# shellcheck disable=SC1090
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

if [ $# -lt 1 ]; then
  echo "Usage: $0 MS_NAME [OPTIONS-for-MS_NAME]"
  exit 1
fi

# 一些关键命令
OPENRESTY_CMD="/usr/local/openresty/nginx/sbin/nginx"
PRESTART_CMD="$(dirname "$0")/bk-codecc-start-pre.sh"

# 检查是否为systemd启动, codecc的systemd配置文件里会设置CODECC_SYSTEMD=1
check_started_by_systemd (){
  [ "${PPID:-0}" = 1 ] && [ -n "${CODECC_SYSTEMD:-}" ]
}

# 检查变量是否赋值. 提供变量名.
check_empty_var (){
  local k e=0
  for k in "$@"; do
    if [ -z "${!k:-}" ]; then
      echo >&2 "var $k is empty or not set."
      ((++e))
    fi
  done
  return $e
}
# 检查进程存活.
check_pid_alive (){
  [ -d "/proc/$1/" ]
}

tip_file_exist (){
  local m="file exist" e=0
  [ -f "$1" ] || { m="file not exist"; e=1; }
  echo "$1: $m."
  return $e
}
tip_dir_exist (){
  local m="dir exist" e=0
  [ -d "$1" ] || { m="dir not exist"; e=1; }
  echo "$1: $m."
  return $e
}

# 启动网关.
start_codecc__openresty (){
  $OPENRESTY_CMD -p "$PWD" -g "user $MS_USER;"
}

check_port_listen (){
  local patt_port_listen port=$1
  printf -v patt_port_listen "^ *[0-9]+: [0-9A-F]+:%X 0+:0+ 0A " "$port"
  if cat /proc/net/tcp /proc/net/tcp6 | grep -E "$patt_port_listen"; then
    echo "ERROR: port $port is LISTENed by others."
    return 19
  fi
  return 0
}
# 探测微服务启动入口
detect_main (){
  # 探测是slim版还是fatjar. 后台启动. PID文件及日志路径保持一致.
  if [ -f "META-INF/MANIFEST.MF" ]; then
    java_env+=("CLASSPATH=$CLASSPATH")
    java_argv+=("-Dfatjar=/$MS_NAME/boot-$MS_NAME.jar")  # 兼容fatjar文件名匹配进程.
    java_run="$MAIN_CLASS"
  elif [ -f "boot-$MS_NAME.jar" ]; then
    java_run="-jar ./boot-$MS_NAME.jar"
  else
    echo >&2 "unsupported codecc-proj dir: $PWD."
    return 31
  fi
}

# 检查服务启动成功. health接口为格式化后的, 要求整行匹配.
check_springboot_up (){
  local port="$1"
  curl -m 1 -sf "http://127.0.0.1:$port/management/health" 2>/dev/null | grep -qx '  "status" : "UP"'
}
# 等待服务启动成功.
wait_springboot_up (){
  local pid="$1" port="$2" msg="app is up. ^_^"
  local wait_count=40 wait_sec=3  # 等待app启动, 否则认为失败触发systemd的自动重启.
  SECONDS=0
  until check_springboot_up "$port"; do
    echo "wait_springboot_up $port: $wait_count: sleep ${wait_sec}s.";
    sleep "$wait_sec";
    let wait_count-- || { msg="wait timeout"; break; }
    check_pid_alive "$pid" || { msg="java is dead"; break; }
  done
  echo "wait_springboot_up $port: $wait_count: $msg."
  if check_springboot_up "$port"; then
    return 0
  else
    return 14
  fi
}

# 启动微服务.
start_codecc__springboot (){
  check_empty_var MEM_OPTS DEVOPS_GATEWAY SPRING_CONFIG_LOCATION JAVA_TOOL_OPTIONS || return 1
  local pid_file="./logs/java.pid" java_pid=0
  local java_env=() java_argv=() java_run="" JAVA_OPTS=${JAVA_OPTS:-}
  # 端口LISTEN预检(重复启动检查).
  check_port_listen "$API_PORT" || return $?
  # 检查启动入口, 设置公共运行参数等.
  detect_main || return $?
  for k in LANG USER HOME SHELL LOGNAME PATH HOSTNAME LD_LIBRARY_PATH ${!JAVA_*} ${!SPRING_*}; do
    if [ -n "${!k-}" ]; then java_env+=("$k=${!k}"); fi  # 如果定义, 则传递.
  done
  java_argv+=(
    "-Ddevops_gateway=$DEVOPS_GATEWAY"
    "-Dserver.port=$API_PORT"  # 强制覆盖配置文件里的端口.
    "-Dbksvc=bk-codecc-$MS_NAME"
  )
  # 指定环境变量及参数, 启动PATH里的java.
  env -i "${java_env[@]}" java -server "${java_argv[@]}" \
    $MEM_OPTS $JAVA_OPTS $java_run &>./logs/bootstrap.log &
  java_pid=$!
  echo "$java_pid" > "$pid_file" || return 24
  echo "java pid is $java_pid."
  # 此处阻塞.
  if ! wait_springboot_up "$java_pid" "$API_PORT"; then
    echo "wait_springboot_up: unable to confirm app status from http://127.0.0.1:$API_PORT/management/health"
    echo "see $PWD/logs/bootstrap.log and $PWD/logs/$MS_NAME.log for details."
    return 14
  fi
}

# 模拟systemd时的预设环境: 加载env, 切换启动目录及用户.
load_systemd_env (){
  local env_file
  set -a
  for env_file in service.env start.env; do
    if [ -f "./$env_file" ]; then
      echo "load env_file: $env_file."
      source "./$env_file" || return 22
    fi
  done
  set +a
}
emulate_systemd_prerequisites (){
  local script_path=$(readlink -f "$0")  # 基于此脚本定位.
  if [ -n "${BK_CODECC_HOME:-}" ]; then
    echo "BK_CODECC_HOME comes from env: $BK_CODECC_HOME."
  else
    export BK_CODECC_HOME=${script_path%/*/*/*}
    echo "guess BK_CODECC_HOME=$BK_CODECC_HOME."
  fi
  cd "$BK_CODECC_HOME/$MS_NAME" || return 16
  load_systemd_env || return $?
  check_empty_var MS_USER || return 15  # env文件里必须定义MS_USER
  # gateway使用root启动nginx, 其worker为普通用户.
  if [ "${USER:-}" != "${MS_USER:-no-user}" ] && [ "$MS_NAME" != "gateway" ]; then
    echo "please run this script using user: ${MS_USER:-}. example command:"
    echo "sudo -u $MS_USER $0 $MS_NAME ..."
    return 5
  fi
  echo "call pre-start: $PRESTART_CMD $*"
  $PRESTART_CMD "$MS_NAME" "$@" || return 20
  load_systemd_env  # systemd允许pre-start修改env. 故重新加载.
}

MS_NAME=${1:-}
shift
# 启动codecc服务. 参数为单个微服务.
# 检查参数的服务名称.
# 如果检查到不是通过systemd调用的, 则调用pre-start.sh
if check_started_by_systemd; then
  echo "launched by systemd."
else
  echo "I am not launched by systemd, try to emulate systemd prerequisites."
  emulate_systemd_prerequisites "$@" || exit $?
fi
ret=0
# 如果存在专属函数, 则调用, 否则使用默认的.
case $MS_NAME in
  gateway) func=start_codecc__openresty;;
  *) func=start_codecc__springboot;;
esac
"$func" "$@" || ret=$? || true  # 收集退出码.
if [ -n "${CODECC_SYSTEMD:-}" ]; then
  sleep 1  # 确保systemd正常回收journal.
fi
exit "$ret"
