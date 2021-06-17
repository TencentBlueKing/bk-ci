#!/bin/bash
# bk-ci 启用或禁用https.
set -eu

#trap 'echo "出错了.";' ERR

ci_env_default="bin/default/ci.env"
ci_env_03="bin/03-userdef/ci.env"
ci_env_04="bin/04-final/ci.env"

CTRL_DIR="${CTRL_DIR:-/data/install}"
BK_PKG_SRC_PATH="${BK_PKG_SRC_PATH:-/data/src}"
bk_cert_source="$BK_PKG_SRC_PATH/cert/bk_domain.crt"
bk_certkey_source="$BK_PKG_SRC_PATH/cert/bk_domain.key"
BK_PKG_SRC_PATH=${BK_CI_SRC_DIR:-/data/src}
BK_CI_SRC_DIR="${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}"  # ci安装源

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

check_ci_env_by_patt (){
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
    echo "本脚本自动修改 $ci_env_03 后未能生效, 可能如下文件中覆盖了对应变量, 请人工修正."
    grep -E "^(${patt_failed_var#|})=" ./bin/*/*.env
    return 1
  fi
}

echo "${1:-}" | grep -qxE "https?" || {
  echo "Usage: $0 https|http -- setup ci-gateway https mode or fallback to http only."
  exit 1
}
target_schema="$1"

ci_schema_vars="BK_HTTP_SCHEMA BK_CI_PUBLIC_URL BK_CI_PAAS_LOGIN_URL"

echo "配置 ci-gateway 为 $target_schema."
tip_file_exist "$ci_env_default"
tip_file_exist "$ci_env_03"
if grep -q ^BK_CI_PAAS_DIALOG_LOGIN_URL "$ci_env_default"; then
  ci_schema_vars="$ci_schema_vars BK_CI_PAAS_DIALOG_LOGIN_URL"
fi
echo "修改env03文件: $ci_env_03"
patt_ci_schema_vars="^(${ci_schema_vars// /|})="
grep -E "$patt_ci_schema_vars" "$ci_env_03"
if [ "$target_schema" = https ]; then
  echo "启用https后, 原 HTTP 入口依旧存在, 无需回退. "
  sed -ri "/$patt_ci_schema_vars/{s@\<http\>@https@;s@:80/@:443/@;}" "$ci_env_03"
else
  echo "禁用https后, 如果因浏览器缓存HTTP重定向到https入口, 请清空浏览器站点数据."
  sed -ri "/$patt_ci_schema_vars/{s@\<https\>@http@;s@:443/@:80/@;}" "$ci_env_03"
fi
grep -E "$patt_ci_schema_vars" "$ci_env_03"
echo "合并env文件."
./bin/merge_env.sh ci >/dev/null || true

echo "加载env文件: $ci_env_04"
source "$ci_env_04" || {
  echo "ERROR: 加载环境变量失败, 请在蓝鲸中控机执行. 或根据报错修正问题."
  exit 1
}
echo "检查ci env"
check_ci_env_by_patt "^$target_schema\\>" $ci_schema_vars

if [ "$target_schema" = https ]; then
  echo "检查证书文件"
  tip_file_exist "$bk_cert_source"
  tip_file_exist "$bk_certkey_source"
  # 基于 BK_CI_FQDN 生成证书主体检查模式. 允许通配符.
  patt_cert_subject="$(sed -r \
    -e 's/^([^.]+)(.*)/(Subject: .*CN=|DNS:)([*]|\1)\2/' \
    -e 's/[.]/[.]/g' <<< "$BK_CI_FQDN")"
  # 需要精确匹配: -w.
  openssl x509 -text -noout -in "$bk_cert_source" | grep -wE "$patt_cert_subject" || {
    echo "证书可能有误, 证书文件 $bk_cert_source 中未能匹配到 '$patt_cert_subject' ."
    exit 1
  }
  echo "同步并安装证书"
  ./bkcli sync cert
  ./bkcli install cert
  echo "检查安装后的证书"
  ci_gateway_cert="$BK_HOME/cert/bk_domain.crt"
  ci_gateway_certkey="$BK_HOME/cert/bk_domain.key"
  pcmd -m ci_gateway "ls -l $ci_gateway_cert $ci_gateway_certkey"
fi

echo "修改 ci-gateway 模板"
nginx_ci_conf="$BK_CI_SRC_DIR/support-files/templates/gateway#core#devops.server.conf"
tip_file_exist "$nginx_ci_conf"
if [ "$target_schema" = https ]; then
  sed -i '/^#  ### ssl config begin ###/,/^#  ### ssl config end ###/s/^#//' "$nginx_ci_conf"
  nginx_ci_ssl="$BK_CI_SRC_DIR/support-files/templates/gateway#core#devops.ssl"
  sed -e "s@^ssl_certificate .*@ssl_certificate $ci_gateway_cert;@" \
      -e "s@^ssl_certificate_key .*@ssl_certificate_key $ci_gateway_certkey;@" \
      support-files/templates/nginx/bk.ssl > "$nginx_ci_ssl"
else
  sed -i '/^  ### ssl config begin ###/,/^  ### ssl config end ###/s/^/#/' "$nginx_ci_conf"
fi
sed -n '/### ssl config begin ###/,/### ssl config end ###/p' "$nginx_ci_conf"

echo "安装 ci-gateway"
./bkcli sync common
./bkcli sync ci
pcmd -m ci_gateway 'cd $CTRL_DIR; export LAN_IP ${!BK_CI_*}; ./bin/install_ci.sh -e ./bin/04-final/ci.env -p "$BK_HOME" -m gateway 2>&1;'

echo "检查配置文件"
pcmd -m ci_gateway 'cd /usr/local/openresty/nginx; ./sbin/nginx -t;'
echo "reload 或 启动服务"
pcmd -m ci_gateway 'cd /usr/local/openresty/nginx; ./sbin/nginx -s reload || ./sbin/nginx;'

if [ "$target_schema" = https ]; then
  echo "测试全部 ci_gateway 节点是否能访问本机的 https 服务."
  curl_cmd="curl -sSvko /dev/null --resolv $BK_CI_FQDN:${BK_CI_HTTPS_PORT:-$BK_HTTPS_PORT}:127.0.0.1 $BK_CI_PUBLIC_URL"
  if ! pcmd -m ci_gateway "$curl_cmd 2>&1 | grep -vE '^([><]|[*] Expire)'; exit \${PIPESTATUS[0]}"; then
    echo "部分或全部节点无法在本机访问 $target_schema 服务, 请排除故障后重试. 或使用本脚本回退到http."
    exit 1
  fi

  echo "测试全部 nginx 节点是否能访问本机的 https 服务."
  curl_cmd="curl -sSvko /dev/null --resolv $BK_PAAS_FQDN:${BK_PAAS_HTTPS_PORT:-$BK_HTTPS_PORT}:127.0.0.1 $BK_CI_PAAS_LOGIN_URL"
  if ! pcmd -m nginx "$curl_cmd 2>&1 | grep -vE '^([><]|[*] Expire)'; exit \${PIPESTATUS[0]}"; then
    echo "注意: 可能 PaaS 未启用 $target_schema, 请及时修正."
  fi
fi

echo "刷新 PaaS 工作台的注册信息."
$CTRL_DIR/bin/bk-ci-reg-paas-app.sh

echo "配置 $target_schema 成功. 请检查更新其他env文件中相应的变量."
