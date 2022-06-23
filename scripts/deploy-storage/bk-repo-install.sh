#!/bin/bash
# shellcheck disable=SC2128
# 安装repo指定模块.

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

set -a
CTRL_DIR="${CTRL_DIR}"
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a

MS_USER=${MS_USER:-blueking}  # 暂不建议修改为其他用户, 此功能未测试.
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_REPO_SRC_DIR="${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}"  # repo安装源

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

# repo安装逻辑.
install_repo__common (){
    check_empty_var BK_REPO_HOME BK_REPO_LOGS_DIR || return 15
    [ -x /usr/bin/jq ] || os_pkg_install jq  # 如果已有jq则无需安装.
    # 安装用户和配置目录
    id -u "$MS_USER" &>/dev/null || \
        useradd -m -c "BlueKing CE User" --shell /bin/bash "$MS_USER"

    local d
    for d in /etc/blueking/env "$BK_REPO_HOME" "$BK_REPO_LOGS_DIR" ; do
        command install -o "$MS_USER" -g "$MS_USER" -m 755 -d "$d"
    done
    cp -r "$BK_REPO_SRC_DIR/scripts" "$BK_REPO_HOME/"
    # install java
    $CTRL_DIR/bin/install_java.sh -p $BK_HOME/java/ -f $BK_PKG_SRC_PATH/java8.tgz 
}

# 目前仅yum安装.
os_pkg_install() {
    local pkg
    echo "os_pkg_install: $*"
    for pkg in "$@"; do
        if ! rpm -q "$pkg" >/dev/null; then
            yum -y install "$pkg"
        fi
    done
}

install_openresty() {
    echo "install openresty"
    os_pkg_install openresty
    # 配置openresty符合蓝鲸需要
    install -d /usr/local/openresty/nginx/conf/conf.d

    # 创建nginx logs目录
    install -m 755 -o blueking -g blueking -d ${BK_HOME}/logs/nginx

# 生成openresty nginx主配置
cat << EOF > /usr/local/openresty/nginx/conf/nginx.conf
user  blueking;
worker_processes  auto;

error_log  ${BK_HOME}/logs/nginx/error.log;

#pid        logs/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    log_format  main  '\$remote_addr - \$remote_user [\$time_local] "\$request" '
        '\$status \$body_bytes_sent "\$http_referer" '
        '"\$http_user_agent" "\$http_x_forwarded_for"';

    access_log  ${BK_HOME}/logs/nginx/access.log  main; 
    sendfile        on;
    #tcp_nopush     on;
    server_tokens   off;

    #keepalive_timeout  0;
    keepalive_timeout  65;
    underscores_in_headers on;

    gzip on;
    gzip_min_length 100;
    gzip_proxied    any;
    gzip_types
        text/css
        text/plain
        text/javascript
        application/javascript
        application/json
        application/x-javascript
        application/xml
        application/xml+rss
        application/xhtml+xml
        application/x-font-ttf
        application/x-font-opentype
        application/vnd.ms-fontobject
        image/svg+xml
        image/x-icon
        application/rss+xml
        application/atom_xml;

    # This just prevents Nginx picking a random default server if it doesn't know which
    # server block to send a request to
    server {
        listen      80 default_server;
        server_name _;
        return      444; # "Connection closed without response"
    }
    include conf.d/*.conf;
EOF

# 生成lograte滚动日志
cat <<EOF > /etc/logrotate.d/nginx
${BK_HOME}/logs/nginx/*log {
    create 0644 blueking blueking
    daily
    rotate 10
    missingok
    notifempty
    compress
    sharedscripts
    postrotate
        /bin/kill -USR1 \`cat /usr/local/openresty/nginx/logs/nginx.pid 2>/dev/null\` 2>/dev/null || true
    endscript
}
EOF

    # 启动openresty
    #systemctl enable --now openresty
    #systemctl status openresty
}

install_repo_gateway (){
    install_openresty || return $?
    cp -rv "$BK_REPO_SRC_DIR/gateway" "$BK_REPO_HOME/"
    cp -rv "$BK_REPO_SRC_DIR/frontend" "$BK_REPO_HOME/"
}

install_repo_auth (){
    cp -rv "$BK_REPO_SRC_DIR/backend" "$BK_REPO_HOME/"
}

install_repo_generic (){
    cp -rv "$BK_REPO_SRC_DIR/backend" "$BK_REPO_HOME/"
}

install_repo_repository (){
    cp -rv "$BK_REPO_SRC_DIR/backend" "$BK_REPO_HOME/"
}

install_repo__common
install_repo_$1
