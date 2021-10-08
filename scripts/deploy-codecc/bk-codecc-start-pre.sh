#!/bin/bash
# 启动slim版的微服务的前置检查.
# pre-start兼容service.env检查及生成.
# 仅当存在微服务名时才检查. 否则返回0.

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
MS_NAME=$1
shift

check_empty_var (){
  local v ks e=0
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

# 等待某个服务可用.
wait_dns_up (){
  local s
  local wait_timeout=${WAIT_TIMEOUT:-60}  # 默认等待60s, 然后触发systemd的自动重启.
  until getent hosts "$@"; do
    s=$((SECONDS/7+1));
    echo "waiting ${s}s for domain name up: $*.";
    sleep "$s";
    if [ "$SECONDS" -gt "$wait_timeout" ]; then
      echo "wait_dns_up: timeout reached."
      return 1
    fi
  done
  return 0
}

# 判断CPU负载, 延迟启动部分服务.
wait_cpu_free (){
  local wait_roll=$1
  local wait_round=$((wait_roll*4)) wait_sec=6 idle_min=60  # 等待round轮, 每轮sec. 预期cpu低于idle_min.
  # 如果为0. 则无需等待.
  if [ "$wait_roll" -eq 0 ]; then return 0; fi
  # 如果无vmstat, 则放弃等待CPU可用.
  if ! command -v vmstat >/dev/null; then
    echo "wait_cpu_free: vmstat command not found, unable to get CPU usage. try to sleep $wait_round*$wait_sec seconds"
    sleep $((wait_round*wait_sec))
    return 0
  fi
  echo "wait_cpu_free: vmstat sampling time is $wait_roll seconds."
  while ((wait_round--)); do
    if vmstat 1 "$((wait_roll+1))" | awk -v t="$idle_min" '{s=$(NF-2)}END{if(s>t)exit(0); else exit(1)}'; then
      echo "wait_cpu_free: round $wait_round: CPU idle was satisfied."
      break
    fi
    echo "wait_cpu_free: $wait_round: CPU is busy, sleep ${wait_sec}s."
    sleep $((wait_sec))
  done
  return 0
}

# assembly是除了dockerhost及agentless外的微服务组合.
prestart_codecc_assembly (){
  true
}

# 校验网关关键配置.
prestart_codecc_gateway (){
  /usr/local/openresty/nginx/sbin/nginx -p "$PWD" -t
}

# 启动批次. 共分7批, 剩余的随机分批.
case $MS_NAME in
  gateway|task|defect) wait_roll=0;;
  codeccjob|report) wait_roll=1;;
  asyncreport|apiquery) wait_roll=2;;
  openapi|quartz) wait_roll=3;;
  *) wait_roll=$((RANDOM%3+4));;
esac
wait_cpu_free "$wait_roll"

MS_NAME_WORD=${MS_NAME//-/_}
prestart_func=prestart_codecc_$MS_NAME_WORD
if declare -f "$prestart_func" >/dev/null; then
  $prestart_func "$@"
else
  echo "no prestart for $MS_NAME, do nothing."
fi


