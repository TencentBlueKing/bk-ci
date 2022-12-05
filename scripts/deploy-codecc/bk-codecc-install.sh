#!/bin/bash
# shellcheck disable=SC2128
# 安装codecc指定模块.

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
BK_PKG_SRC_PATH=${BK_CODECC_SRC_DIR:-/data/src}
BK_CODECC_SRC_DIR="${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}"  # codecc安装源

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
# 依赖的安装
install_java() {
  echo "check java"
  # 建议考虑直接使用蓝鲸的 bin/install_java.sh脚本安装. 具体参考蓝鲸文档.
  local java_exe="/usr/bin/java"
  if ! [ -x "$java_exe" ]; then
    os_pkg_install java-1.8.0-openjdk   # 如无则默认使用openjdk.
  fi
}
# 暂时保留此函数.
install_openresty() {
  echo "install openresty"
  os_pkg_install openresty
}

# CODECC安装逻辑.
install_codecc__common (){
  #check_empty_var BK_CODECC_HOME BK_CODECC_LOGS_DIR BK_CODECC_DATA_DIR || return 15
  # 安装用户和配置目录
  id -u "$MS_USER" &>/dev/null || \
    useradd -m -c "BlueKing CE User" --shell /bin/bash "$MS_USER"

  #os_pkg_install jq
  local d
  for d in /etc/blueking/env "$BK_CODECC_HOME" "$BK_CODECC_LOGS_DIR" "${BK_CODECC_DATA_DIR%/?*}"; do
    command install -o "$MS_USER" -g "$MS_USER" -m 755 -d "$d"
  done
  for d in "$BK_CODECC_DATA_DIR"; do
    command install -o "$MS_USER" -g "$MS_USER" -m 750 -d "$d"
  done
}

install_codecc__ms_common (){
  local proj=$1
  #check_empty_var BK_CODECC_SRC_DIR BK_CODECC_HOME BK_CODECC_LOGS_DIR BK_CODECC_DATA_DIR || return 15
  echo >&2 "check installer src: $BK_CODECC_SRC_DIR/$MS_NAME"
  tip_dir_exist "$BK_CODECC_SRC_DIR/$MS_NAME" || return 16
  # 检查安装java
  install_java || return $?
  # 增量复制.
  rsync -ra --delete "$BK_CODECC_SRC_DIR/$MS_NAME/" "$BK_CODECC_HOME/$MS_NAME"
  for f in agent-package jars-public jars-private scripts VERSION; do
    [ -e "$BK_CODECC_SRC_DIR/$f" ] || continue
    echo "install $BK_CODECC_SRC_DIR/$f to $BK_CODECC_HOME."
    rsync -ra --delete "$BK_CODECC_SRC_DIR/${f%/}" "$BK_CODECC_HOME"
  done
  # 保持微服务部分子目录的强一致性.
  rsync -ra --delete "$BK_CODECC_SRC_DIR/$MS_NAME/lib" "$BK_CODECC_SRC_DIR/$MS_NAME/com" "$BK_CODECC_HOME/$MS_NAME"
  # CodeCC启动jar包依赖顺序特殊调整
  mkdir -p $BK_CODECC_HOME/$MS_NAME/priority
  ls -al $BK_CODECC_HOME/$MS_NAME/lib/|grep codecc-common 2>/dev/null && mv $BK_CODECC_HOME/$MS_NAME/lib/codecc-common-*.jar $BK_CODECC_HOME/$MS_NAME/priority/
  sed -i -e '/CLASSPATH/s/.*/CLASSPATH=\".:priority\/*:lib\/*\"/' $BK_CODECC_HOME/$MS_NAME/service.env
}

# 仅需复制frontend目录.
install_codecc_gateway (){
  local proj=$1
  install_openresty || return $?
  
  # 特殊文件处理
  if ! grep -w repo $CTRL_DIR/install.config|grep -v ^\# ; then
    > $BK_CODECC_SRC_DIR/support-files/templates/gateway\#core\#vhosts\#devops.bkrepo.upstream.conf
  else
    cat > $BK_CODECC_SRC_DIR/support-files/templates/gateway\#core\#vhosts\#devops.bkrepo.upstream.conf << EOF 
upstream __BK_REPO_HOST__ {
    server __BK_REPO_GATEWAY_IP__;
}
EOF
  fi

  cat > $BK_CODECC_SRC_DIR/support-files/templates/gateway\#core\#server.common.conf << EOF
  client_max_body_size 0;

  if (\$time_iso8601 ~ '(\d{4}-\d{2}-\d{2})') {
    set \$log_date \$1;
  }

  #dns指向本地的consul dns服务
  resolver __BK_CODECC_GATEWAY_DNS_ADDR__ valid=30s;

  #设置通用变量
  include set.conf;

  #将错误统一处理成json格式返回
  include error/error.html.handler.conf;
  include error/error.json.handler.conf;
  include error/error.json.conf;

  #状态监测
  include nginx.status.conf;

  #网关auth验证
  include auth.conf;
EOF

  rsync -ra "$BK_CODECC_SRC_DIR/gateway" "$BK_CODECC_HOME"  # gateway无需--del
  rsync -ra --delete "$BK_CODECC_SRC_DIR/frontend" "$BK_CODECC_HOME"  # frontend不必verbose.
  if [ -d "$BK_CODECC_SRC_DIR/docs" ]; then
    rsync -ra --delete "$BK_CODECC_SRC_DIR/docs" "$BK_CODECC_HOME" || return $?  # 可选docs
  fi
}

MS_NAME=$1
shift
# 检查环境变量.
check_empty_var BK_CODECC_SRC_DIR BK_CODECC_HOME BK_CODECC_LOGS_DIR BK_CODECC_DATA_DIR
# 本脚本设计为快速滚动更新. 故不会主动新增部署, 仅处理已有的proj.
# 判断服务是否启用.
is_sd_service_enabled (){
  systemctl is-enabled "bk-codecc-$1" &>/dev/null
}
# 判断是否启用了服务, 如果未启用, 则提示显示启用的方法.
if ! is_sd_service_enabled "$MS_NAME"; then
  echo "NOTE: service $MS_NAME not enabled in this node. run bk-codecc-reg-systemd.sh to enable it."
  exit 0
fi
install_codecc__common
MS_NAME_WORD=${MS_NAME//-/_}
install_func=install_codecc_$MS_NAME_WORD
if declare -f "$install_func" >/dev/null; then
  echo "INFO: installer is $install_func."
  $install_func "$MS_NAME" "$@"
else
  echo "INFO: using default installer for codecc micro-service."
  install_codecc__ms_common "$MS_NAME" "$@"
fi
