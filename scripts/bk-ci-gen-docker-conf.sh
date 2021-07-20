#!/bin/bash
# shellcheck disable=SC2128
# 调整docker的配置, 本脚本会保留conf_daemon原本的配置, 仅调整必须的配置key.
# 建议先行覆盖conf_daemon内容作为模板. 然后重新执行本脚本.

set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

if [ $# -lt 1 ]; then
  echo "Usage: $0 /path/to/ci-docker-data-root"
  exit 1
fi
if ! command -v jq >/dev/null; then
  echo "command not found: jq, you should install jq-1.5 and above."
  exit 5
fi
if ! jq -V | grep -qE "^jq-1[.]([5-9]|[1-9][0-9]+)"; then
  echo "jq version is too low, please upgrade to jq-1.5 and above. jq -V:"
  jq -V
  exit 5
fi
docker_data_root=$1
conf_daemon="/etc/docker/daemon.json"
conf_backup="/etc/docker/daemon.json.$(date +%Y%m%d-%H%M%S).bak"

# 合并2个json.
merge_json (){
  if [ $# -lt 2 ]; then
    echo >&2 "Usage: merge_json json_template_file json_overlay_file"
  fi
  # 当文件1为空时, 则-s的数组只有1个元素, 导致.[1]为null.
  jq -s '.[0] * (.[1] // {})' "$1" "$2"
}

## 配置依赖的服务
gen_docker_conf (){
  local conf_text_new data_root="$1"
  # 修改daemon.json.
  conf_text_new=$(merge_json "$conf_daemon" - <<EOF
{
    "data-root": "$data_root",
    "iptables": true,
    "live-restore": true,
    "ip-forward": true
}
EOF
)
  # 对比配置文件. 确保diff时的格式统一.
  #echo "new conf is $conf_text_new."
  if [ -z "$conf_text_new" ]; then
    echo "ERROR gen_docker_conf: failed to merge docker config. check syntax of $conf_daemon:"
    jq . "$conf_daemon"
    return 31
  elif diff -q <(jq . "$conf_daemon") <(echo "$conf_text_new") &>/dev/null; then  # 格式化后对比
    echo "INFO gen_docker_conf: $conf_daemon without change, do nothing."
  else
    echo "INFO gen_docker_conf: $conf_daemon has changed, backup is $conf_backup."
    mv "$conf_daemon" "$conf_backup"
    echo "$conf_text_new" > "$conf_daemon" || {
      echo "ERROR gen_docker_conf: failed to write new conf: $conf_daemon"
      return 1
    }
    # 仅尝试reload. 也不做强制状态检查.
    if ! systemctl reload docker.service; then
      echo "WARNING gen_docker_conf: failed to reload docker.service. you should reload or restart it later."
    fi
  fi
}

# 检查docker是否有被其他软件占用(以修改data-root计).
mkdir -p "$(dirname "$conf_daemon")"
[ -f "$conf_daemon" ] || echo '{}' > "$conf_daemon"
old_data_root=$(jq -r '.["data-root"] // ""' "$conf_daemon")
data_root_default="/var/lib/docker"
case "$old_data_root" in
  ""|"$data_root_default"|"$docker_data_root"|*/docker-bkci|*/docker-bkci/)  # 仅默认修改这些data-root.
    gen_docker_conf "$docker_data_root"
    ;;
  *)
    echo "ERROR: data-root conflict. It maybe used by others, I will not modify it."
    echo "$conf_daemon: data-root is $old_data_root."
    echo "TIPS: you may remove the key or set value to default($data_root_default), and run me again."
    exit 9
    ;;
esac
