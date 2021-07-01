#!/bin/bash
# 禁用systemd服务并删除systemd模板.

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

systemd_unit_dir="${SYSTEMD_UNIT_DIR:-/usr/lib/systemd/system}"

unreg_systemd_codecc (){
  local proj svc_name sd_service sd_target
  proj="$1"  # codecc组件名. 对应目录之类的.
  svc_name=bk-codecc-$proj  # 完整服务名, 用于命令行标记等.
  sd_service="$svc_name.service"  # systemd 服务名
  sd_target="bk-codecc.target"  # systemd target名.
  echo "disable and stop service: $sd_service."
  systemctl disable --now "$sd_service"  # 禁用并停止.
  if [ -f "$systemd_unit_dir/$sd_service" ]; then
    rm "$systemd_unit_dir/$sd_service" || return 3
  fi
  # 判断是否需要删除target.
  shopt -s nullglob
  a_sd_service_files=( "$systemd_unit_dir/bk-codecc-"*.service )
  shopt -u nullglob
  if [ ${#a_sd_service_files[@]} -eq 0 ]; then
    echo "no codecc service remaining, disable target $sd_target."
    systemctl disable --now "$sd_target"  # 禁用并停止.
    if [ -f "$systemd_unit_dir/$sd_target" ]; then
      rm "$systemd_unit_dir/$sd_target" || return 3
    fi
  fi
}

# 检查脚本用法.
if [ $# -lt 1 ]; then
  echo >&2 "Usage: $0 proj...   -- unregister systemd service for bk-codecc projects."
  exit 1
fi
# 检查systemd
if ! [ -d "$systemd_unit_dir" ]; then
  echo >&2 "ERROR: your OS does not support systemd: unit dir does not exsit: $systemd_unit_dir."
  exit 4
fi
# 检查当前是否启动着systemd.
if [ -d /proc/1/ ] && grep -q systemd /proc/1/cmdline; then
  :
else
  echo >&2 "ERROR: your OS does not booted with systemd: PID 1 should be systemd."
  exit 4
fi

for MS_NAME in "$@"; do
  unreg_systemd_codecc "$MS_NAME"
done

