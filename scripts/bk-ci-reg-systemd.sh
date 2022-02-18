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

BK_CI_SRC_DIR="${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}"
sd_ci_target="bk-ci.target"
# 剥离dns重定向能力为独立服务, 默认启用.
sd_dns_redirect="bk-ci-docker-dns-redirect.service"
sd_docker="docker.service"
sd_consul="consul.service"

# 强依赖, 如果依赖的服务退出/重启, 则本服务退出. 但是依赖的服务启动, 本服务不会自动启动.
declare -A systemd_service_requires=(
  ["agentless"]="$sd_docker"
  ["dockerhost"]="$sd_docker"
  ["default"]=""
)
# 弱依赖, 期望依赖服务存活但不强求, 也允许依赖服务退出或重启.
declare -A systemd_service_wants=(
  ["dockerhost"]="$sd_dns_redirect"
  ["agentless"]="$sd_dns_redirect"
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

gen_systemd_ci__target (){
  # 生成bk-ci.target
  systemd_unit_set bk-ci.target <<EOF
[Unit]
Description=BK CI target could start/stop all bk-ci-*.service at once

[Install]
WantedBy=blueking.target multi-user.target
EOF
}

# 强依赖consul, 如果目的不存在, 应该果断退出. 且docker启动后才创建, 所以需要after.
gen_systemd_ci__docker_dns_redirect (){
  systemd_unit_set "$sd_dns_redirect" <<EOF
[Unit]
Description=Blueking CI - dns traffic redirect for docker
Requires=$sd_consul
After=$sd_docker $sd_consul
PartOf=$sd_ci_target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment=NIC_DEVICE=docker0
Environment=CONTAINER_DNS_DPORT=53
Environment=DNS_CACHE_SERVER=127.0.0.1:53
# 用于iptables规则描述及匹配. 无空格.
Environment=IPTABLES_RULE_MARKER=bk-ci-docker-dns-hijack
EnvironmentFile=-$sysconfig_dir/${sd_dns_redirect%.service}
ExecStartPre=/bin/bash -c 'command -v iptables'
ExecStart=/usr/bin/env sysctl -w net.ipv4.ip_forward=1
ExecStart=/usr/bin/env sysctl -w net.ipv4.conf.\${NIC_DEVICE}.route_localnet=1
ExecStart=/bin/bash -c 'if iptables -t nat -S PREROUTING | grep -- "\${IPTABLES_RULE_MARKER}"; then echo "iptables rule exist yet, do nothing."; else iptables -t nat -I PREROUTING 1 -i "\${NIC_DEVICE}" -p udp --dport "\${CONTAINER_DNS_DPORT}" -j DNAT --to-destination "\${DNS_CACHE_SERVER}" -m comment --comment "\${IPTABLES_RULE_MARKER}"; fi'
ExecStop=/bin/bash -c 'if iptables -t nat -S PREROUTING | grep -- "\${IPTABLES_RULE_MARKER}"; then echo "iptables rule exist, cleanup it."; iptables -t nat -D PREROUTING -i "\${NIC_DEVICE}" -p udp --dport "\${CONTAINER_DNS_DPORT}" -j DNAT --to-destination "\${DNS_CACHE_SERVER}" -m comment --comment "\${IPTABLES_RULE_MARKER}"; else echo "iptables rule was cleaned already, do nothing."; fi'
EOF
}

gen_systemd_ci_gateway (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking CI - $sd_service
After=network-online.target $sd_requires $sd_wants
Requires=$sd_requires
Wants=$sd_wants
PartOf=$sd_ci_target

[Service]
Type=forking
PIDFile=$BK_CI_HOME/$proj/run/nginx.pid
PassEnvironment=PATH HOSTNAME
Environment=CI_SYSTEMD=1
EnvironmentFile=$sysconfig_dir/$svc_name
WorkingDirectory=$BK_CI_HOME/$proj
ExecStart=/bin/bash $BK_CI_HOME/scripts/bk-ci-start.sh $proj
ExecStartPre=/bin/bash $BK_CI_HOME/scripts/bk-ci-start-pre.sh $proj
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
PrivateTmp=true
Restart=always
RestartSec=5
LimitNOFILE=204800

[Install]
WantedBy=$sd_ci_target
EOF
}
gen_systemd_ci__ms (){
  # 参考springboot官方模板.
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking CI - $proj
After=network-online.target $sd_requires $sd_wants
Requires=$sd_requires
Wants=$sd_wants
PartOf=$sd_ci_target

[Service]
Type=forking
PIDFile=$BK_CI_HOME/$proj/logs/java.pid
PassEnvironment=PATH HOSTNAME
Environment=CI_SYSTEMD=1
EnvironmentFile=$BK_CI_HOME/$proj/service.env
EnvironmentFile=$sysconfig_dir/$svc_name
WorkingDirectory=$BK_CI_HOME/$proj
User=$MS_USER
ExecStart=/bin/bash $BK_CI_HOME/scripts/bk-ci-start.sh $proj
ExecStartPre=/bin/bash $BK_CI_HOME/scripts/bk-ci-start-pre.sh $proj
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
WantedBy=$sd_ci_target
EOF
}

reg_systemd_ci (){
  proj="$1"  # ci组件名. 对应目录之类的.
  svc_name=bk-ci-$proj  # 完整服务名, 用于命令行标记等.
  sd_service="$svc_name.service"  # systemd 服务名
  sd_requires=${systemd_service_requires[$proj]:-${systemd_service_requires[default]}}
  sd_wants=${systemd_service_wants[$proj]:-${systemd_service_wants[default]}}
  #load env:
  case $proj in
    gateway)
      gen_systemd_ci_gateway "$proj"
      ;;
    agentless|dockerhost)
      gen_systemd_ci__docker_dns_redirect  # 注册dns重定向脚本, 但是无需启用.
      ;&
    *)
      MS_USER=${MS_USER:-blueking}
      gen_systemd_ci__ms "$proj"
      ;;
  esac
  update_link_to_target "$sysconfig_dir/$svc_name" "$BK_CI_HOME/$proj/start.env"
}

# 检查脚本用法.
if [ $# -lt 1 ]; then
  echo >&2 "Usage: $0 proj...   -- register systemd service for bk-ci projects."
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
# 检查BK_CI_HOME
if [ -z "${BK_CI_HOME:-}" ]; then
  echo >&2 "ERROR: env BK_CI_HOME is not set or empty. please set it first."
  exit 2
fi

invalid_proj=""
for MS_NAME in "$@"; do
  [ -d "$BK_CI_SRC_DIR/$MS_NAME" ] || {
    echo "dir $BK_CI_SRC_DIR/$MS_NAME not eixst."
    invalid_proj="$invalid_proj,$MS_NAME"
  }
done
if [ "${#invalid_proj}" -gt 1 ]; then
  echo "ERROR: invalid proj: ${invalid_proj:1}."  # 去掉开头的逗号.
  exit 15
fi

gen_systemd_ci__target
for MS_NAME in "$@"; do
  reg_systemd_ci "$MS_NAME"
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
systemctl enable bk-ci.target
for MS_NAME in "$@"; do
  svc_name=bk-ci-$MS_NAME  # 完整服务名, 用于命令行标记等.
  sd_service="$svc_name.service"  # systemd 服务名
  systemctl enable "$sd_service"
done
