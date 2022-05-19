#!/bin/bash
# shellcheck disable=SC2128
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
BK_PKG_SRC_PATH=${BK_REPO_SRC_DIR:-/data/src}
BK_REPO_SRC_DIR="${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}"  # repo安装源
CTRL_DIR=${CTRL_DIR:-/data/install}
LAN_IP=${LAN_IP:-$(ip route show | grep -Pom 1 "(?<=src )[0-9.]+")}
source ${CTRL_DIR:-/data/install}/load_env.sh

BKCE_RENDER_CMD="$CTRL_DIR/bin/render_tpl"  # 蓝鲸社区版的render, 需要env文件及$BK_HOME.
REPO_RENDER_CMD="$(dirname "$0")/render_tpl"  # bk-repo里的默认读取本地的bkenv.properties文件.
GEN_DOCKER_CONF_CMD="$(dirname "$0a")/bk-repo-gen-docker-conf.sh"

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

# 负责渲染repo.
render_repo (){
    local proj=$1
    local files=()
    shopt -s nullglob
    case "$proj" in
        gateway)
          # 渲染可能存在的gateway配置文件.
          files+=("$BK_REPO_SRC_DIR/support-files/templates/gateway#"*)
          ;&  # 这里不中断, 继续渲染frontend.
        frontend)
          # 渲染可能存在的frontend页面文件.
          files+=("$BK_REPO_SRC_DIR/support-files/templates/frontend#"*)
          ;;
        *)
          # 渲染对应的微服务配置文件. 这里的模式必须通配到.
          files+=("$BK_REPO_SRC_DIR/support-files/templates/#etc#repo#application.yaml"
            "$BK_REPO_SRC_DIR/support-files/templates/#etc#repo#"*"$proj."*
            "$BK_REPO_SRC_DIR/support-files/templates/$proj"#*
            )
          ;;
    esac
    shopt -u nullglob
    if [ "${#files[@]}" -eq 0 ]; then
        echo "render_repo: no file matches, do nothing, proj is $proj."
        return 5
    fi
    if [ -x "$BKCE_RENDER_CMD" ]; then
        BK_ENV_FILE="$CTRL_DIR/bin/04-final/repo.env" $BKCE_RENDER_CMD -u -p "$BK_HOME" -m repo "${files[@]}"
        #echo "$BKCE_RENDER_CMD -u -p $BK_HOME -m repo -e /data/install/bin/04-final/repo.env ${files[@]}"
        #$BKCE_RENDER_CMD -u -p "$BK_HOME" -m repo -e /data/install/bin/04-final/repo.env "${files[@]}"
        echo "render success"
    elif [ -x "$REPO_RENDER_CMD" ]; then
        $REPO_RENDER_CMD -m repo "${files[@]}"
    else
        echo >&2 "REPO_RENDER_CMD is not executable: $REPO_RENDER_CMD."
        return 1
    fi
}

## 配置repo
# 微服务公共配置.
setup_repo__ms_common (){
    check_empty_var BK_REPO_HOME BK_REPO_LOGS_DIR BK_REPO_DATA_DIR || return 15
    # 判断目录是否存在
    #tip_dir_exist "$MS_DIR" || return 16
    echo "$MS_LOGS_DIR" "$MS_DATA_DIR" "$MS_DIR"
    mkdir -p "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 1
    chown -R "$MS_USER:$MS_USER" "$MS_LOGS_DIR" "$MS_DATA_DIR" || return 57
    render_repo "$MS_NAME" || return $?
}

setup_repo_auth (){
    local proj=$1
    setup_repo__ms_common "$proj" || return 11
    echo "Repo - $proj setup"
    # 注册 repo-auth.service.consul
    if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
        check_empty_var BK_REPO_AUTH_PORT LAN_IP || return 15
        $CTRL_DIR/bin/reg_consul_svc -n repo-auth -p ${BK_REPO_AUTH_PORT} -a $LAN_IP -D > /etc/consul.d/service/repo-auth.json 2>/dev/null || return 11
        consul reload
    else
      echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: repo-auth.service.consul"
    fi
}

setup_repo_generic (){
    local proj=$1
    setup_repo__ms_common "$proj" || return 11
    echo "Repo - $proj setup"
    # 注册 repo-generic.service.consul
    if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
        check_empty_var BK_REPO_GENERIC_PORT LAN_IP || return 15
        $CTRL_DIR/bin/reg_consul_svc -n repo-generic -p ${BK_REPO_GENERIC_PORT} -a $LAN_IP -D > /etc/consul.d/service/repo-generic.json 2>/dev/null || return 11
        consul reload
    else
      echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: repo-generic.service.consul"
    fi
}

setup_repo_repository (){
    local proj=$1
    setup_repo__ms_common "$proj" || return 11
    echo "Repo - $proj setup"
    # 注册 repo-repository.service.consul
    if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
        check_empty_var BK_REPO_REPOSITORY_PORT LAN_IP || return 15
        $CTRL_DIR/bin/reg_consul_svc -n repo-repository -p ${BK_REPO_REPOSITORY_PORT} -a $LAN_IP -D > /etc/consul.d/service/repo-repository.json 2>/dev/null || return 11
        consul reload
    else
      echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: repo-repository.service.consul"
    fi
}

setup_repo__gw_start_env (){
    #check_empty_var BK_REPO_HOME || return 15
    local start_env="$MS_DIR/start.env"
    env_line_set "$start_env" "MS_USER" "$MS_USER"
    chown "$MS_USER:$MS_USER" "$start_env" || return 5
}

# 校验网关关键配置, 设置家目录, 设置启动用户或setcap?
setup_repo_gateway (){
    #check_empty_var BK_REPO_DATA_DIR BK_REPO_HOME BK_REPO_LOGS_DIR || return 15
    setup_repo__gw_start_env || return $?
    # 判断nginx.conf路径
    local gateway_dir="$BK_REPO_HOME/gateway" nginx_conf='' nginx_conf_dir=''
    if [ -f "$gateway_dir/core/nginx.conf" ]; then
        nginx_conf="$gateway_dir/core/nginx.conf"
        nginx_conf_dir="$gateway_dir/core"
    elif [ -f "$gateway_dir/nginx.conf" ]; then
        nginx_conf="$gateway_dir/nginx.conf"
        nginx_conf_dir="$gateway_dir"
    else
        echo >&2 "ERROR: unsupported repo-gateway dir."
        return 4
    fi
    # 更新conf目录的指向.
    update_link_to_target "$gateway_dir/conf" "$nginx_conf_dir" || return 3
    # 创建并更新logs目录.
    mkdir -p "$BK_REPO_LOGS_DIR/nginx" || return 2
    chown -R "$MS_USER:$MS_USER" "$BK_REPO_LOGS_DIR/nginx" || return 5
    update_link_to_target "$gateway_dir/logs" "$BK_REPO_LOGS_DIR/nginx" || return 3
    update_link_to_target "$gateway_dir/run" "$BK_REPO_LOGS_DIR/nginx" || return 3
    # 在数据目录创建运行时的存储目录, 并更新链接.
    local gateway_data_dir="$BK_REPO_DATA_DIR/gateway" temp_dir=
    mkdir -p "$gateway_data_dir" || return 1
    for temp_dir in client_body_temp fastcgi_temp proxy_temp scgi_temp uwsgi_temp files; do
        mkdir -p "$gateway_data_dir/$temp_dir" || return 2
        chown "$MS_USER:$MS_USER" "$gateway_data_dir/$temp_dir" || return 5
        update_link_to_target "$gateway_dir/$temp_dir" "$gateway_data_dir/$temp_dir" || return 3
    done
    # 检查 bk_login_v3 的lua文件, 如果没有, 就使用v2的替代.
    #if [ "${BK_REPO_AUTH_PROVIDER:-sample}" = "bk_login_v3" ]; then
    #    if ! cp -nv "$nginx_conf_dir/lua/auth/auth_user-bk_login.lua" \
    #        "$nginx_conf_dir/lua/auth/auth_user-bk_login_v3.lua"; then
    #        echo >&2 "failed to prepare auth_user-bk_login_v3.lua"
    #        return 6
    #    fi
    #fi
    # prod目录指向agent-package.
    # 预期200: curl -I bk-repo.service.consul/static/files/jar/worker-agent.jar
    update_link_to_target "$gateway_data_dir/files/prod" "$BK_REPO_HOME/agent-package"
    # 在全部 repo-gateway 节点上注册主入口域名: bk-repo.service.consul, 用于在集群内提供web服务.
    if [ -x $CTRL_DIR/bin/reg_consul_svc ]; then
        check_empty_var LAN_IP || return 15
        $CTRL_DIR/bin/reg_consul_svc -n bk-repo -p ${BK_REPO_HTTP_PORT:-80} -a "$LAN_IP" -D > /etc/consul.d/service/bk-repo.json 2>/dev/null || return 11
        consul reload
    else
        echo "$CTRL_DIR/bin/reg_consul_svc is not executable, skip register domain: bk-repo.service.consul."
    fi
    # 渲染gateway配置及frontend页面.
    render_repo "$MS_NAME" || return $?

    # 创建nginx依赖目录
    install -m 755 -o blueking -g blueking -d $BK_HOME/logs/nginx
    install -m 755 -o blueking -g blueking -d $BK_HOME/logs/repo/nginx
    install -m 755 -d /usr/local/openresty/nginx/run/
    if [[ -d /tmp/conf ]] ; then rm -rf /tmp/conf ; mv /usr/local/openresty/nginx/conf /tmp/ ; fi
    ln -sf $BK_HOME/repo/gateway /usr/local/openresty/nginx/conf || return 7
}

MS_NAME=$1
shift
# 检查环境变量
check_empty_var BK_REPO_HOME BK_REPO_DATA_DIR BK_REPO_LOGS_DIR BK_REPO_CONSUL_DISCOVERY_TAG

# 本脚本设计为快速滚动更新. 故不会主动新增部署, 仅处理已有的proj.
# 判断服务是否启用.
is_sd_service_enabled (){
    systemctl is-enabled "bk-repo-$1" &>/dev/null
}

# 判断是否启用了服务, 如果未启用, 则提示显示启用的方法.
if ! is_sd_service_enabled "$MS_NAME"; then
    echo "NOTE: service $MS_NAME not enabled in this node. run bk-repo-reg-systemd.sh to enable it."
    exit 0
fi

# 公共变量
MS_NAME_WORD=${MS_NAME//-/_}
MS_DIR="$BK_REPO_HOME/$MS_NAME"
MS_LOGS_DIR="$BK_REPO_LOGS_DIR/$MS_NAME"
MS_DATA_DIR="$BK_REPO_DATA_DIR/$MS_NAME"
BK_HOME="${BK_HOME:-$(readlink -f "$BK_REPO_HOME/..")}"
BK_REPO_CONF_DIR="${BK_REPO_CONF_DIR:-$BK_HOME/etc/repo}"
service_env="$CTRL_DIR/bin/04-final/repo.env"
start_env="$CTRL_DIR/bin/04-final/repo.env"
setup_func=setup_repo_$MS_NAME_WORD

if declare -f "$setup_func" >/dev/null; then
    echo "$setup_func starting"
    $setup_func "$MS_NAME" "$@"
else
    echo "INFO: $setup_func not defined, try to call default handler.."
    setup_repo__ms_common "$MS_NAME" "$@"
fi

