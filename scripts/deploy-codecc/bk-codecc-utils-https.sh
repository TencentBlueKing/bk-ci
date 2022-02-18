#!/bin/bash
# bk-codecc 启用或禁用https.
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

codecc_env_default="bin/default/codecc.env"
codecc_env_03="bin/03-userdef/codecc.env"
codecc_env_04="bin/04-final/codecc.env"

CTRL_DIR="${CTRL_DIR:-/data/install}"
BK_PKG_SRC_PATH="${BK_PKG_SRC_PATH:-/data/src}"
bk_cert_source="$BK_PKG_SRC_PATH/cert/bk_domain.crt"
bk_certkey_source="$BK_PKG_SRC_PATH/cert/bk_domain.key"
BK_PKG_SRC_PATH=${BK_CODECC_SRC_DIR:-/data/src}
BK_CODECC_SRC_DIR="${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}"  # codecc安装源

ip add | grep -qwf .controller_ip || {
  echo "本脚本应该在蓝鲸中控机运行."
  exit 1
}
cd "$CTRL_DIR" || {
  echo "ABORT: failed to cd $CTRL_DIR."
  exit 1
}

tip_file_exist (){
  local m="文件存在" e=0
  [ -f "$1" ] || { m="文件不存在"; e=1; }
  echo "$m: $1."
  return $e
}

pcmd (){
  ./pcmd.sh "$@"
  return $?
}

check_codecc_env_by_patt (){
  local patt="$1"
  local var value patt_failed_var="" e=0
  shift
  for var in "$@"; do
    value="${!var}"
    echo -n "检查 $var($value) 应匹配正则($patt). "
    if echo "$value" | grep -Eq "$patt"; then
      echo "通过"
    else
      echo "失败";
      patt_failed_var="$patt_failed_var|$var";
      let ++e;
    fi
  done
  if [ $e -gt 0 ]; then
    echo "本脚本自动修改 $codecc_env_03 后未能生效, 可能如下文件中覆盖了对应变量, 请人工修正."
    grep -E "^(${patt_failed_var#|})=" ./bin/*/*.env
    return 1
  fi
}

echo "${1:-}" | grep -qxE "https?" || {
  echo "Usage: $0 https|http -- setup codecc-gateway https mode or fallback to http only."
  exit 1
}
target_schema="$1"

codecc_schema_vars="BK_HTTP_SCHEMA BK_CODECC_PUBLIC_URL BK_CI_PUBLIC_URL"

source "$CTRL_DIR/load_env.sh"
echo "配置 codecc-gateway 为 $target_schema."
tip_file_exist "$codecc_env_default"
tip_file_exist "$codecc_env_03"
echo "修改env03文件: $codecc_env_03"
if grep -q "[$]BK_CI_PUBLIC_URL" "$codecc_env_03"; then
  echo "检查到 \$BK_CI_PUBLIC_URL 变量引用, 自动替换."
  sed -ri "s@[$]BK_CI_PUBLIC_URL@$BK_CI_PUBLIC_URL@" "$codecc_env_03"  # 替换默认设置的变量引用.
fi
patt_codecc_schema_vars="^(${codecc_schema_vars// /|})="
grep -E "$patt_codecc_schema_vars" "$codecc_env_03"
if [ "$target_schema" = https ]; then
  echo "启用https后, 原 HTTP 入口依旧存在, 一般无需回退."
  sed -ri "/$patt_codecc_schema_vars/{s@\<http\>@https@;s@:80/@:443/@;}" "$codecc_env_03"
else
  echo "禁用https后, 如果因浏览器缓存HTTP重定向到https入口, 请清空浏览器站点数据."
  sed -ri "/$patt_codecc_schema_vars/{s@\<https\>@http@;s@:443/@:80/@;}" "$codecc_env_03"
fi
grep -E "$patt_codecc_schema_vars" "$codecc_env_03"
echo "合并env文件."
./bin/merge_env.sh codecc >/dev/null || true

echo "加载env文件: $codecc_env_04"
source "$codecc_env_04" || {
  echo "ERROR: 加载环境变量失败, 请在蓝鲸中控机执行. 或根据报错修正问题."
  exit 1
}
echo "检查codecc env"
check_codecc_env_by_patt "^$target_schema\\>" $codecc_schema_vars

if [ "$target_schema" = https ]; then
  echo "检查证书文件"
  tip_file_exist "$bk_cert_source"
  tip_file_exist "$bk_certkey_source"
  # 基于 BK_CODECC_FQDN 生成证书主体检查模式. 允许通配符.
  echo "检查证书域名"
  patt_cert_subject="$(sed -r \
    -e 's/^([^.]+)(.*)/(Subject: .*CN=|DNS:)([*]|\1)\2/' \
    -e 's/[.]/[.]/g' <<< "$BK_CODECC_FQDN")"
  # 需要精确匹配: -w.
  openssl x509 -text -noout -in "$bk_cert_source" | grep -wE "$patt_cert_subject" || {
    echo "证书可能有误, 证书文件 $bk_cert_source 中未能匹配到 '$patt_cert_subject'. 证书支持包含的名称如下:"
    openssl x509 -text -noout -in "$bk_cert_source" | grep -E "^ *(Subject|DNS):"
    exit 1
  }
  echo "同步并安装证书"
  ./bkcli sync cert
  ./bkcli install cert
  echo "检查安装后的证书"
  codecc_gateway_cert="$BK_HOME/cert/bk_domain.crt"
  codecc_gateway_certkey="$BK_HOME/cert/bk_domain.key"
  pcmd -m codecc_gateway "ls -l $codecc_gateway_cert $codecc_gateway_certkey"
fi

echo "修改 codecc-gateway 模板"
nginx_codecc_conf="$BK_CODECC_SRC_DIR/support-files/templates/gateway#core#vhosts#codecc.server.conf"
tip_file_exist "$nginx_codecc_conf"
patt_ssl_config_commented='/^ *# *### ssl config begin ###/,/^ *# *### ssl config end ###/'
patt_ssl_config_nocomment='/^ *### ssl config begin ###/,/^ *### ssl config end ###/'
if [ "$target_schema" = https ]; then
  sed -ri "${patt_ssl_config_commented:-^#####}s/^( *)#/\\1/" "$nginx_codecc_conf"  # 移除注释
  nginx_codecc_ssl="$BK_CODECC_SRC_DIR/support-files/templates/gateway#core#devops.ssl"
  sed -e "s@^ssl_certificate .*@ssl_certificate $codecc_gateway_cert;@" \
      -e "s@^ssl_certificate_key .*@ssl_certificate_key $codecc_gateway_certkey;@" \
      support-files/templates/nginx/bk.ssl > "$nginx_codecc_ssl"
  echo "检查修改结果."
  sed -n "${patt_ssl_config_nocomment:-^#####}p" "$nginx_codecc_conf" | grep "^ *listen [_A-Z0-9].* ssl;"
else
  sed -i "${patt_ssl_config_nocomment:-^#####}s/^/#/" "$nginx_codecc_conf"
  echo "检查修改结果."
  sed -n "${patt_ssl_config_commented:-^#####}p" "$nginx_codecc_conf" | grep "^ *# *listen [_A-Z0-9].* ssl;"
fi

echo "重新配置 codecc-gateway"
./bkcli sync common
./bkcli sync codecc
pcmd -m codecc_gateway 'source ${CTRL_DIR:-/data/install}/load_env.sh; export LAN_IP ${!BK_*}; ${BK_PKG_SRC_PATH:-/data/src}/codecc/scripts/deploy-codecc/bk-codecc-setup.sh gateway'

echo "reload 或 启动服务"
pcmd -m codecc_gateway 'systemctl reload bk-codecc-gateway || systemctl start bk-codecc-gateway'

if [ "$target_schema" = https ]; then
  echo "测试全部 codecc_gateway 节点是否能访问本机的 https 服务."
  curl_cmd="curl -sSvko /dev/null --resolv $BK_CODECC_FQDN:${BK_CODECC_HTTPS_PORT:-443}:127.0.0.1 $BK_CODECC_PUBLIC_URL"
  if ! pcmd -m codecc_gateway "$curl_cmd 2>&1 | grep -vE '^([><]|[*] Expire)'; exit \${PIPESTATUS[0]}"; then
    echo "部分或全部节点无法在本机访问 $target_schema 服务, 请排除故障后重试. 或使用本脚本回退到http."
    exit 1
  fi

  echo "测试 ci-gateway 节点是否能访问本机的 https 服务."
  curl_cmd="curl -sSvko /dev/null --resolv $BK_CI_FQDN:${BK_CI_HTTPS_PORT:-443}:127.0.0.1 $BK_CI_PUBLIC_URL"
  if ! pcmd -m ci_gateway "$curl_cmd 2>&1 | grep -vE '^([><]|[*] Expire)'; exit \${PIPESTATUS[0]}"; then
    echo "注意: 可能 CI 未启用 $target_schema, 请及时修正."
  fi
fi

echo "刷新 ci-nav ."
${BK_PKG_SRC_PATH:-/data/src}/codecc/scripts/deploy-codecc/bk-codecc-reg-ci-nav.sh

echo "配置 $target_schema 成功. 请检查更新其他env文件中相应的变量."
