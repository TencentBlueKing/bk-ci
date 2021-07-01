#!/bin/bash
# 升级或部署当前节点上的指定实例.
# shellcheck disable=SC2219

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
  echo "Usage: $0 MS_NAME..."
  exit 1
fi

INSTALL_CMD="$(dirname "$0")/bk-codecc-install.sh"
SETUP_CMD="$(dirname "$0")/bk-codecc-setup.sh"


# 如何反查服务实例端口?

# 注册/解注册spring实例.
spring_instance_set_status (){  # Usage: STATUS PROJS...
  return 0
}
# 查询实例状态.
spring_instance_check_status (){  # Usage: STATUS PROJS...
  return 0
}

is_sd_service_exist (){
  systemctl cat "$1" &>/dev/null
}
is_sd_service_enabled (){
  systemctl is-enabled "$1" &>/dev/null
}
is_sd_service_alive (){
  systemctl is-active "$1" >/dev/null
}

log (){
  echo "$(date +%Y%m%d-%H%M%S) ${LOG_LEVEL:-INFO} $*"
}
err (){
  LOG_LEVEL=ERROR log "$@"
}

instance_offline_status="OUT_OF_SERVICE"
instance_online_status="UP"

#sd_target=bk-codecc.target
declare -A sd_service_map=()
declare -A svc_name_map=()
declare -a enabled_proj=()
echo "本脚本传入的参数: $*."
for proj in "$@"; do
  proj=${proj#codecc@}  # strip掉前缀的codecc@
  svc_name=bk-codecc-$proj
  sd_service=$svc_name.service
  if is_sd_service_enabled "$sd_service"; then
    enabled_proj+=("$proj")
    sd_service_map[$proj]="$sd_service"
    svc_name_map[$proj]="$svc_name"
  else
    echo "跳过本机未启用的服务: $proj"
  fi
done
if [ ${#enabled_proj[@]} -eq 0 ]; then
  echo "指定的服务均未启用. 退出码为5."
  exit 5
else
  echo "本机启用的服务为: ${enabled_proj[*]}, 下述操作仅处理这些服务."
fi

for proj in "${enabled_proj[@]}"; do
  sd_service=${sd_service_map[$proj]}
  svc_name=${svc_name_map[$proj]}
  log "后台停止服务: $sd_service."
  systemctl stop "$sd_service" &  # 直接后台systemctl, 方便后续wait.
done

for proj in "${enabled_proj[@]}"; do
  sd_service=${sd_service_map[$proj]}
  svc_name=${svc_name_map[$proj]}
  log "安装服务: $proj"
  "$INSTALL_CMD" "$proj"
done
for proj in "${enabled_proj[@]}"; do
  sd_service=${sd_service_map[$proj]}
  svc_name=${svc_name_map[$proj]}
  log "配置服务: $proj"
  "$SETUP_CMD" "$proj"
done

log "等待服务停止完毕."
wait

# java启动时耗费大量CPU, 需控制并发, 防止systemd启动超时自动重启.
if [ "${CONCURRENT_FACTOR:-0}" -ge 1 ]; then
  log "from env, CONCURRENT_FACTOR=$CONCURRENT_FACTOR."
elif [ -r /proc/cpuinfo ]; then
  CONCURRENT_FACTOR=$(gawk '/^processor.:/{c++;} END{c=c/2>1?int(c/2):1; print c}' /proc/cpuinfo)
  log "base on cpuinfo, CONCURRENT_FACTOR=$CONCURRENT_FACTOR."
else
  CONCURRENT_FACTOR=1
  log "unable to determine, set CONCURRENT_FACTOR=$CONCURRENT_FACTOR."
fi
CONCURRENT_SLEEP=${CONCURRENT_SLEEP:-6}
CPU_LIMIT_SLEEP=${CPU_LIMIT_SLEEP:-10}
# 检查是否限制频率.
last_throttle_sec=0
has_been_throttled (){
  local throttled_sec=0 cpu_stat="/sys/fs/cgroup/cpu/cpu.stat"
  if [ -r "$cpu_stat" ]; then
    throttled_sec=$(awk '/throttled_time/{print int($2/10**9)}' "$cpu_stat")
  fi
  if [ "$throttled_sec" -gt "$last_throttle_sec" ]; then
    last_throttle_sec="$throttled_sec"
    return 0
  else
    return 1  # 默认为1. 无限制.
  fi
}
n=0
declare -A pid_in_bg=()
declare -A pending_services=()
for proj in "${enabled_proj[@]}"; do
  if has_been_throttled; then
    log "检测到触发CGroup CPU限制, 等待${CPU_LIMIT_SLEEP}s."
    sleep "$CPU_LIMIT_SLEEP"
  fi
  sd_service=${sd_service_map[$proj]}
  svc_name=${svc_name_map[$proj]}
  systemctl start "$sd_service" >/dev/null 2>&1 &
  pending_services["$sd_service"]=1
  pid_in_bg[$!]="$sd_service"
  log "后台启动服务: $sd_service, pid=$!"
  ((++n))
done
log "等待全部服务启动完毕."
wait

log "检查服务是否启动成功."
check_codecc_service_up (){
  local e=0 sd_service
  for sd_service in "${!pending_services[@]}"; do
    is_sd_service_alive "$sd_service" && unset pending_services["$sd_service"] || let ++e
  done
  return $e
}
wait=${BKI_UPGRADE_WAIT:-30}
until check_codecc_service_up; do
  sleep 1
  echo -n "${#pending_services[@]} "
  let wait-- || { echo ""; log "等待超时"; break; }
done
if [ -n "${!pending_services[*]}" ]; then
  echo "如下服务未能及时启动: ${!pending_services[*]}."
  exit 19
else
  echo ""
fi

#log "上线服务实例"
#spring_instance_set_status "$instance_online_status" "${!svc_name_map[@]}"
#
#log "检查是否上线成功."
#wait=${BKI_UPGRADE_WAIT:-60}
#until spring_instance_check_status "$instance_online_status" "${!svc_name_map[@]}" >/dev/null; do
#  sleep 1;
#  let wait-- || { log "等待超时"; break; }
#done
if spring_instance_check_status "$instance_online_status" "${!svc_name_map[@]}" >/dev/null; then
  log "全部服务启动成功. 耗时: $SECONDS."
else
  log "全部或部分服务启动失败. 耗时: $SECONDS. 退出码为3."
  exit 3
fi
