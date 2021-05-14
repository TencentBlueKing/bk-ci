#!/bin/bash

set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

die (){ echo >&2 "$@"; exit 1; }
command -v jq >/dev/null || die "command not found: jq"
SELF_DIR=$(readlink -f "$(dirname "$0")")
ESB_API_TOOL="$SELF_DIR/esb_api_test.sh"
BK_BIZ_ID=2
BK_CLOUD_ID=0
source "${CTRL_DIR-:/data/install}/load_env.sh" || die "unable to load bk env."

ci_set_tpl_id=$($ESB_API_TOOL POST /api/c/compapi/v2/cc/list_set_template/ '"bk_biz_id": '$BK_BIZ_ID | jq -r 'select(.data).data.info[] | select(.name == "ci") | .id')
[ -z "$ci_set_tpl_id" ] && die "unable to get set_tpl_id of ci."

get_ci_set_id (){
  BK_SET_ID=$($ESB_API_TOOL POST /api/c/compapi/v2/cc/search_biz_inst_topo/ '"level": 0, "bk_biz_id": '$BK_BIZ_ID | jq -r 'select(.data).data[]|.child[] | select(.bk_obj_id=="set" and .bk_inst_name=="蓝盾").bk_inst_id')
  [ -n "$BK_SET_ID" ]
}

echo "获取ci集群id"
if ! get_ci_set_id; then
  $ESB_API_TOOL POST /api/c/compapi/v2/cc/create_set/ '"bk_biz_id": '$BK_BIZ_ID', "data": { "bk_parent_id": '$BK_BIZ_ID', "set_template_id": '$ci_set_tpl_id', "bk_set_name": "蓝盾", "bk_set_desc": "bk-ci", "description": "蓝鲸持续集成平台（蓝盾），bk-ci"}' || true
  get_ci_set_id || die "unable to get ci_set_id."
fi
echo "bk_set_id(ci)=$BK_SET_ID"

eval declare -A MAP_SVC_TPL_ID=(
  $($ESB_API_TOOL POST /api/c/compapi/v2/cc/list_service_template/ '"bk_biz_id": '$BK_BIZ_ID | jq -r '.data.info[] | select(.name|match("^ci-")) | "[\(.name)]=\(.id)"')
)

eval declare -A MAP_BK_MODULE_ID=(
  $($ESB_API_TOOL POST /api/c/compapi/v2/cc/search_biz_inst_topo/ '"level": 0, "bk_biz_id": '$BK_BIZ_ID | jq -r '.data[].child[] | select(.bk_obj_id=="set" and .bk_inst_name=="蓝盾") as $set | .child[] | "[\(.bk_inst_name)]=\(.bk_inst_id)"')
)

get_host_id_by_ip (){
  [ $# -ge 1 ] || { echo "Usage: get_host_id_by_ip IP"; return 1; }
  $ESB_API_TOOL POST /api/c/compapi/v2/cc/list_hosts_without_biz/ '"page":{"start":0, "limit": 1}, "fields": [ "bk_host_id", "bk_cloud_id", "bk_host_innerip"],"host_property_filter": {"condition": "AND","rules": [ { "field": "bk_host_innerip", "operator": "equal", "value": "'$1'"}, { "field": "bk_cloud_id", "operator": "equal", "value": 0}]}' | jq -r '.data | select(.info).info[] | .bk_host_id'
}

list_host_in_module (){
  [ $# -ge 1 ] || { echo "Usage: list_host_in_module BK_MODULE_ID"; return 1; }
  eval $($ESB_API_TOOL POST /api/c/compapi/v2/cc/find_host_by_topo/ '"bk_biz_id": '$BK_BIZ_ID', "bk_obj_id": "module", "bk_inst_id": '$1', "fields": ["bk_host_id", "bk_host_innerip"], "page": { "start": 0, "limit": 100 }' | jq -r '.data | select(.info).info[] | "'"${LIST_MAP_NAME:-MAP_HOST_ID}"'[\(.bk_host_innerip)]=\(.bk_host_id)"')
}

add_ip_to_business (){
  [ $# -ge 1 ] || { echo "Usage: add_ip_to_business IP"; return 1; }
  $ESB_API_TOOL POST /api/c/compapi/v2/cc/add_host_to_resource/ '"bk_biz_id": '$BK_BIZ_ID', "host_info": { "0":  { "bk_host_innerip": "'$1'", "import_from": "3", "bk_cloud_id": 0 }}'
}
add_host_to_module (){
  [ $# -ge 2 ] || { echo "Usage: add_host_to_module BK_HOST_IDs BK_MODULE_IDs"; return 1; }
  $ESB_API_TOOL POST /api/c/compapi/v2/cc/transfer_host_module/ '"bk_biz_id": '$BK_BIZ_ID', "is_increment": true, "bk_host_id": ['$1'], "bk_module_id": ['$2']'
}

echo "注册IP到蓝鲸业务: $BK_CI_IP_COMMA"
declare -A MAP_CI_HOST
for ip in "${BK_CI_IP[@]}"; do
  add_ip_to_business "$ip" &>/dev/null
  MAP_CI_HOST[$ip]=$(get_host_id_by_ip "$ip")
  if [ -z "${MAP_CI_HOST[$ip]:-}" ]; then
    die "unable to get bk_host_id of IP: $ip"
  else
    echo "bk_host_id($ip)=${MAP_CI_HOST[$ip]:-}"
  fi
done

echo "处理CI组件"
msg_ok_new="\033[32;1m(ok-NEW)\033[m"
msg_ok_exist="\033[32m(ok-exist)\033[m"
msg_fail="\033[31;1m(fail)\033[m"
msg_skip_install="  SKIP(not-in-install.config)\n"
msg_skip_cmdb="\033[33m  SKIP(CMDB-no-such-module)\033[m\n"
for mod in gateway dockerhost agentless artifactory auth dispatch environment image log misc notify openapi plugin process project quality repository store ticket websocket; do
  mod_name=ci-$mod
  printf "  %-20s" "$mod_name"
  mod_ip_var=BK_CI_${mod^^}_IP[*]
  ips=${!mod_ip_var:-}
  if [ -z "$ips" ]; then
    printf "$msg_skip_install"
    continue
  fi
  bk_module_id=${MAP_BK_MODULE_ID[$mod_name]:-}
  if [ -z "$bk_module_id" ]; then
    printf "$msg_skip_cmdb"
    continue
  fi
  declare -A MAP_HOST_ID=()
  list_host_in_module "$bk_module_id"
  for ip in ${ips//,/ }; do
    echo -n "  $ip"
    if [ -z "${MAP_HOST_ID[$ip]:-}" ]; then
      add_host_to_module "${MAP_CI_HOST[$ip]}" "$bk_module_id" &>/dev/null
      declare -A MAP_HOST_ID=()
      list_host_in_module "$bk_module_id"
      [ -z "${MAP_HOST_ID[$ip]:-}" ] && printf "$msg_fail" || printf "$msg_ok_new"
    else
      printf "$msg_ok_exist"
    fi
  done
  echo ""
done

