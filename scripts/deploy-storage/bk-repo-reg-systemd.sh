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

CTRL_DIR="${CTRL_DIR}"
source ${CTRL_DIR:-/data/install}/load_env.sh
BK_REPO_SRC_DIR="${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}"
sd_repo_target="bk-repo.target"

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

gen_systemd_repo__target (){
    # 生成bk-repo.target
    systemd_unit_set bk-repo.target <<EOF
[Unit]
Description=BK repo target could start/stop all bk-repo-*.service at once

[Install]
WantedBy=blueking.target multi-user.target
EOF
}

gen_systemd_repo__ms (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking repo - $proj
After=network-online.target $sd_requires $sd_wants
Requires=$sd_requires
Wants=$sd_wants
PartOf=$sd_repo_target

[Service]
Type=simple
PIDFile=$BK_REPO_HOME/$proj/pid/$svc_name.pid
PassEnvironment=PATH HOSTNAME
Environment=REPO_SYSTEMD=1
EnvironmentFile=-$sysconfig_dir/$svc_name
WorkingDirectory=$BK_REPO_HOME/$proj
User=$MS_USER
ExecStart=$BK_REPO_HOME/$proj/$svc_name -f conf.json
ExecStartPre=/bin/bash $BK_REPO_HOME/scripts/deploy-repo/bk-repo-start-pre.sh $proj
ExecStop=/bin/kill -s TERM \$MAINPID
SuccessExitStatus=0
StandardOutput=journal
StandardError=journal
LimitNOFILE=204800
LimitCORE=infinity
TimeoutStopSec=35
TimeoutStartSec=300
Restart=always
RestartSec=10

[Install]
WantedBy=$sd_repo_target
EOF
}

gen_systemd_repo_gateway (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking Repo - $proj
After=syslog.target network.target remote-fs.target nss-lookup.target
PartOf=$sd_repo_target

[Service]
WorkingDirectory=/usr/local/openresty/nginx/
Type=forking
PIDFile=/usr/local/openresty/nginx/run/nginx.pid
ExecStartPre=/usr/local/openresty/nginx/sbin/nginx -p /usr/local/openresty/nginx -g 'user blueking;' -t -c /usr/local/openresty/nginx/conf/nginx.conf
ExecStart=/usr/local/openresty/nginx/sbin/nginx -p /usr/local/openresty/nginx -g 'user blueking;'
ExecReload=/usr/local/openresty/nginx/sbin/nginx -p /usr/local/openresty/nginx -g 'user blueking;' -s reload
ExecStop=/bin/kill -s QUIT \$MAINPID
PrivateTmp=true
Restart=on-failure

[Install]
WantedBy=$sd_repo_target
EOF
}

gen_systemd_repo_auth (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking Repo - $proj
Requires=network-online.target
After=consul-client.target
PartOf=$sd_repo_target

[Service]
EnvironmentFile=-/usr/bin/java
Environment=GOMAXPROCS=2
Restart=on-failure
ExecStart=/usr/bin/java -server -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Xms512m -Xmx1024m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$BK_REPO_LOGS_DIR/auth-gc.log -jar $BK_REPO_HOME/backend/service-auth.jar --spring.profiles.active=prod
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
KillSignal=SIGTERM

[Install]
WantedBy=$sd_repo_target
EOF
}

gen_systemd_repo_generic (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking Repo - $proj
Requires=network-online.target
After=consul-client.target
PartOf=$sd_repo_target

[Service]
EnvironmentFile=-/usr/bin/java
Environment=GOMAXPROCS=2
Restart=on-failure
ExecStart=/usr/bin/java -server -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Xms512m -Xmx1024m -jar $BK_REPO_HOME/backend/service-generic.jar --spring.profiles.active=dev --spring.cloud.consul.port=8500
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
KillSignal=SIGTERM

[Install]
WantedBy=$sd_repo_target
EOF
}

gen_systemd_repo_repository (){
  systemd_unit_set "$sd_service" <<EOF
[Unit]
Description=Blueking Repo - $proj
Requires=network-online.target
After=consul-client.target
PartOf=$sd_repo_target

[Service]
EnvironmentFile=-/usr/bin/java
Environment=GOMAXPROCS=2
Restart=on-failure
ExecStart=/usr/bin/java -server -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Xms512m -Xmx1024m -jar $BK_REPO_HOME/backend/service-repository.jar --spring.profiles.active=dev --spring.cloud.consul.port=8500
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s QUIT \$MAINPID
KillSignal=SIGTERM

[Install]
WantedBy=$sd_repo_target
EOF
}

reg_systemd_repo (){
    proj="$1"  # repo组件名. 对应目录之类的.
    svc_name=bk-repo-$proj  # 完整服务名, 用于命令行标记等.
    sd_service="$svc_name.service"  # systemd 服务名
    sd_requires=''
    sd_wants=''
    #load env:
    case $proj in
        gateway)
            MS_USER=${MS_USER:-blueking}
            gen_systemd_repo_gateway
            ;;
        auth)
            MS_USER=${MS_USER:-blueking}
            gen_systemd_repo_auth
            ;;
        generic)
            MS_USER=${MS_USER:-blueking}
            gen_systemd_repo_generic
            ;;
        repository)
            MS_USER=${MS_USER:-blueking}
            gen_systemd_repo_repository
            ;;
        #*)
        #    MS_USER=${MS_USER:-blueking}
        #    gen_systemd_repo__ms "$proj"
        #    ;;
    esac
}

# 检查脚本用法.
if [ $# -lt 1 ]; then
    echo >&2 "Usage: $0 proj... -- register systemd service for bk-repo projects."
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

# 检查BK_REPO_HOME
if [ -z "${BK_REPO_HOME:-}" ]; then
    echo >&2 "ERROR: env BK_REPO_HOME is not set or empty. please set it first."
    exit 2
fi

gen_systemd_repo__target
for MS_NAME in "$@"; do
    reg_systemd_repo "$MS_NAME"
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
systemctl enable bk-repo.target
for MS_NAME in "$@"; do
    svc_name=bk-repo-$MS_NAME  # 完整服务名, 用于命令行标记等.
    sd_service="$svc_name.service"  # systemd 服务名
    systemctl enable "$sd_service"
done

