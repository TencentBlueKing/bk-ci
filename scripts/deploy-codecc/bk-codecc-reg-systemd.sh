#!/bin/bash
# 生成systemd模板.

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

BK_CODECC_SRC_DIR="${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}"
sd_codecc_target="bk-codecc.target"
# 剥离dns重定向能力为独立服务, 默认启用.
sd_dns_redirect="bk-codecc-docker-dns-redirect.service"
sd_docker="docker.service"
sd_consul="consul.service"

# 强依赖, 如果依赖的服务退出/重启, 则本服务退出. 但是依赖的服务启动, 本服务不会自动启动.
declare -A systemd_service_requires=(
  ["default"]=""
)
# 弱依赖, 期望依赖服务存活但不强求, 也允许依赖服务退出或重启.
declare -A systemd_service_wants=(
  ["default"]="$sd_consul"
)
# After, 决定启动次序. 等声明的目标服务启动结束后, 才会启动本服务.
# 不单独定义 systemd_service_after, 取 systemd_service_requires 和 systemd_service_wants 合集.

sysconfig_dir="${SYSCONFIG_DIR:-/etc/sysconfig}"
systemd_unit_dir="${SYSTEMD_UNIT_DIR:-/usr/lib/systemd/system}"


# 创建或覆盖unit文件
systemd_unit_set (){
  local unit_name="$1"
  local conf="$systemd_unit_dir/$unit_name"
  echo "generating systemd unit file: $conf"
  # 如果存在空白定义行, 则过滤掉.
  grep -Ev "^[a-zA-Z][a-zA-Z0-9_]*=[ \t]*$" > "$conf"
  chown root:root "$conf"
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

gen_systemd_codecc__target (){
  # 生成bk-codecc.target
  systemd_unit_set bk-codecc.target <<EOF
[Unit]
Description=BK CODECC target could start/stop all bk-codecc-*.service at once

[Install]
WantedBy=blueking.target multi-user.target
EOF
}

gen_systemd_codecc_gateway (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking CODECC - $proj
After=network-online.target $sd_requires $sd_wants
Requires=$sd_requires
Wants=$sd_wants
PartOf=$sd_codecc_target

[Service]
Type=forking
PIDFile=$BK_CODECC_HOME/$proj/run/nginx.pid
PassEnvironment=PATH HOSTNAME
Environment=CI_SYSTEMD=1
EnvironmentFile=$sysconfig_dir/$svc_name
WorkingDirectory=$BK_CODECC_HOME/$proj
ExecStart=/bin/bash $BK_CODECC_HOME/scripts/deploy-codecc/bk-codecc-start.sh $proj
ExecStartPre=/bin/bash $BK_CODECC_HOME/scripts/deploy-codecc/bk-codecc-start-pre.sh $proj
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
PrivateTmp=true
Restart=always
RestartSec=5
LimitNOFILE=204800

[Install]
WantedBy=$sd_codecc_target
EOF
}

gen_systemd_codecc__ms (){
  # 参考springboot官方模板.
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking CODECC - $proj
After=network-online.target $sd_requires $sd_wants
Requires=$sd_requires
Wants=$sd_wants
PartOf=$sd_codecc_target

[Service]
Type=forking
PIDFile=$BK_CODECC_HOME/$proj/logs/java.pid
PassEnvironment=PATH HOSTNAME
Environment=CODECC_SYSTEMD=1
EnvironmentFile=$BK_CODECC_HOME/$proj/service.env
EnvironmentFile=$sysconfig_dir/$svc_name
WorkingDirectory=$BK_CODECC_HOME/$proj
User=$MS_USER
ExecStart=/bin/bash $BK_CODECC_HOME/scripts/deploy-codecc/bk-codecc-start.sh $proj
ExecStartPre=/bin/bash $BK_CODECC_HOME/scripts/deploy-codecc/bk-codecc-start-pre.sh $proj
ExecStop=/bin/kill -s TERM \$MAINPID
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
LimitNOFILE=204800
LimitCORE=infinity
TimeoutStopSec=35
TimeoutStartSec=300
Restart=always
RestartSec=10

[Install]
WantedBy=$sd_codecc_target
EOF
}

reg_systemd_codecc (){
  proj="$1"  # codecc组件名. 对应目录之类的.
  svc_name=bk-codecc-$proj  # 完整服务名, 用于命令行标记等.
  sd_service="$svc_name.service"  # systemd 服务名
  sd_requires=${systemd_service_requires[$proj]:-${systemd_service_requires[default]}}
  sd_wants=${systemd_service_wants[$proj]:-${systemd_service_wants[default]}}
  #load env:
  case $proj in
    gateway)
      gen_systemd_codecc_gateway "$proj"
      ;;
    *)
      MS_USER=${MS_USER:-blueking}
      gen_systemd_codecc__ms "$proj"
      ;;
  esac
  update_link_to_target "$sysconfig_dir/$svc_name" "$BK_CODECC_HOME/$proj/start.env"
}

# 检查脚本用法.
if [ $# -lt 1 ]; then
  echo >&2 "Usage: $0 proj...   -- register systemd service for bk-codecc projects."
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
# 检查sysconfig.
if ! [ -d "$sysconfig_dir" ]; then
  echo >&2 "ERROR: sysconfig_dir does not exsit: $sysconfig_dir, this script support CentOS/RHEL only."
  exit 4
fi
# 检查BK_CODECC_HOME
if [ -z "${BK_CODECC_HOME:-}" ]; then
  echo >&2 "ERROR: env BK_CODECC_HOME is not set or empty. please set it first."
  exit 2
fi

invalid_proj=""
for MS_NAME in "$@"; do
  if [ "$MS_NAME" = "gateway" ]; then continue; fi  # codecc-gateway 复用ci目录.
  [ -d "$BK_CODECC_SRC_DIR/$MS_NAME" ] || {
    echo "dir $BK_CODECC_SRC_DIR/$MS_NAME not eixst."
    invalid_proj="$invalid_proj,$MS_NAME"
  }
done
if [ "${#invalid_proj}" -gt 1 ]; then
  echo "ERROR: invalid proj: ${invalid_proj:1}."  # 去掉开头的逗号.
  exit 15
fi

gen_systemd_codecc__target
for MS_NAME in "$@"; do
  reg_systemd_codecc "$MS_NAME"
done
systemctl (){  # 规避systemctl daemon-reload及reenable报错.
  local sd_retry=9
  until command systemctl "$@" ; do
  let sd_retry-- || { echo "max retry exceed."; break; }
    echo "systemctl $* failed, sleep 1 and retry..."
    sleep 1
  done
}
# 重新加载生成的配置文件.
systemctl daemon-reload
# 启用target.
systemctl enable bk-codecc.target
for MS_NAME in "$@"; do
  svc_name=bk-codecc-$MS_NAME  # 完整服务名, 用于命令行标记等.
  sd_service="$svc_name.service"  # systemd 服务名
  systemctl enable "$sd_service"
done
