#!/bin/bash
# 2020-05-07
# operate bkci build machine.
tip (){ echo >&2 "$@";}
die (){ tip "$@"; exit 1;}
debug (){ [ -n "$DEBUG" ] && tip -e "\033[7mDEBUG\033[0m" "$@"; }

today=$(date +%Y-%m-%d)

CTRL_DIR=${CTRL_DIR:-/data/install}

dispatch_api_prefix="http://127.0.0.1:21922/api/op/dispatchDocker"

jq_filter_get=".id,.dockerIp,.dockerHostPort,.enable,.capacity,.createTime"
jq_filter_get_v=".id,.dockerIp,.dockerHostPort,.enable,.grayEnv,.specialOn,.capacity,.usedNum,.averageCpuLoad,.averageMemLoad,.averageDiskLoad,.averageDiskIOLoad,.createTime"

header_uid="X-DEVOPS-UID: admin"
header_json="Content-Type: application/json"
curl='curl -sS'
# $1: method
# $2: url
# $N: addition options.
curl_helper (){
  #debug "$FUNCNAME called: $*"
  local method=$1
  local uri=$2
  shift 2
  [ -z "$method" -o -z "$uri" ] && die "Usage: $FUNCNAME HTTP_METHOD URI [addition_curl_options]..."
  debug $curl -X$method -H "$header_uid" -H "$header_json" "$dispatch_api_prefix/$uri" "$@"
  $curl -X$method -H "$header_uid" -H "$header_json" "$uri" "$@"
}

pretty_tsv (){
  local TAB=$'\t'
  local sep=${1:-$TAB}
  # stdout是tty才能pretty. 否则保持原样.
  [ -t 1 ] && column -ts "$sep" || cat
}

pretty_json (){
  jq '.'
}

usage_cmd_del="$0 del IP id=N"
kvpatt_cmd_del="^id="
cmd_del (){
  local ip=$1
  shift
  [ -z "$ip" -o $# -eq 0 ] && die "Usage: $usage_cmd_del"
  local e=0
  local _data enable id dockerHostPort capacity
  for kv in "$@"; do
    [[ "$kv" =~ $kvpatt_cmd_set ]] || { tip "unknow kv: kv($kv) does NOT match patt($kvpatt_cmd_add)."; let e++; continue; }
    local $kv && let s++
  done
  # id必选, 用于校验.
  [ -z "$id" ] && die "id must be set, for validation. you can use '$0 list' to get the id."
  read old_id old_ip old_dockerHostPort old_enable old_capacity __ < <(cmd_list | awk -v id="$id" -v ip="$ip" '$1==id||$2==ip')
  # 校验提供的id, ip与现存是否相符, 为了防止IP填写有误.
  tip "validate id($id) and ip($ip)."
  #cmd_list | grep "^$id $ip " || {
  [ "$old_id" == "$id" -a "$old_ip" == "$ip" ] || {
  tip "  id and ip does not match. possible entry is:"
    cmd_list | awk -v id="$id" -v ip="$ip" '$1==id||$2==ip'
    die ""
  }
  curl_helper "DELETE" "$dispatch_api_prefix/delete/$ip" | pretty_json
}

usage_cmd_add="$0 add IP enable=true|false [dockerHostPort=21923] [capacity=100]"
kvpatt_cmd_add="^(dockerHostPort|capacity|enable)="
json_tpl_cmd_add='[{"dockerIp":"%s","dockerHostPort":%d,"capacity":%d,"enable":%s,"createTime":"%s"}]'
cmd_add (){
  local ip=$1
  shift
  [ -z "$ip" -o $# -eq 0 ] && die "Usage: $usage_cmd_add"
  local e=0
  local _data enable dockerHostPort capacity
  for kv in "$@"; do
    [[ "$kv" =~ $kvpatt_cmd_add ]] || { tip "unknow kv: kv($kv) does NOT match patt($kvpatt_cmd_add)."; let e++; continue; }
    local $kv
  done
  [ "$enable" = "true" -o "$enable" = "false" ] || die "enable must set to true or false."
  [ $e -gt 0 ] && die "arg parse error. quit."
  printf -v _data "$json_tpl_cmd_add" "$ip" "${dockerHostPort:-21923}" \
    "${capacity:-100}" "$enable" "$today"
  curl_helper POST "$dispatch_api_prefix/add" -d "$_data" | pretty_json
}

usage_cmd_set="$0 set IP id=N KEY=VALUE..."
kvpatt_cmd_set="^(dockerHostPort|id|enable|capacity)="
json_tpl_cmd_set='{"id":%d,"dockerIp":"%s","dockerHostPort":%d,"capacity":%d,"enable":%s,"createTime":"%s"}'
cmd_set (){
  local ip=$1
  shift
  [ -z "$ip" -o $# -eq 0 ] && die "Usage: $usage_cmd_set"
  local _data enable id dockerHostPort capacity
  local e=0 s=0
  for kv in "$@"; do
    [[ "$kv" =~ $kvpatt_cmd_set ]] || { tip "unknow kv: kv($kv) does NOT match patt($kvpatt_cmd_add)."; let e++; continue; }
    local $kv && let s++
  done
  # id必选, 用于校验.
  [ -z "$id" ] && die "id must be set, for validation. you can use '$0 list' to get the id."
  # 除去id外, 必须设置其他项目.
  [ $s -lt 2 ] && die "no key specified, do nothing."
  [ -n "$enable" ] && {
    [ "$enable" = "true" -o "$enable" = "false" ] || die "enable must set to true or false."
  }
  # 读取旧值.
  read old_id old_ip old_dockerHostPort old_enable old_capacity __ < <(cmd_list | awk -v id="$id" -v ip="$ip" '$1==id||$2==ip')
  # 校验提供的id, ip与现存是否相符, 为了防止IP填写有误. (好像多此一举?)
  tip "validate id($id=$old_id) and ip($ip=$old_ip)."
  #cmd_list | grep "^$id $ip " || {
  [ "$old_id" = "$id" -a "$old_ip" = "$ip" ] || {
    # 一些提示, 一丝温暖.
    tip "  id and ip does not match. possible entry is:"
    cmd_list | awk -v id="$id" -v ip="$ip" '$1==id||$2==ip'
    die ""
  }
  printf -v _data "$json_tpl_cmd_set" "$id" "$ip" \
    "${dockerHostPort:-$old_dockerHostPort}" \
    "${capacity:-$old_capacity}" "${enable:-$old_enable}" "$today"
  curl_helper PUT "$dispatch_api_prefix/update/$ip" -d "$_data" | pretty_json
}

get_records_tsv (){
  jq -r ".data.records[] | [$jq_filter_get] | @tsv"
}

cmd_list (){
  [ "$1" == "-v" ] && local jq_filter_get="$jq_filter_get_v"
  local header=${jq_filter_get//./}
  [ -t 1 ] && tty_pretty="column -t" || tty_pretty="cat"
  { echo -e "${header//,/\t}";
    curl_helper "GET" "$dispatch_api_prefix/getDockerIpList?pageSize=9999" | get_records_tsv
  } | pretty_tsv
}

usage="Usage: $0 list|set|add|del [args...]"
[ -z "$1" ] && die "$usage"
command -v curl >/dev/null || die "ERROR: command not found: curl."
command -v jq >/dev/null || die "ERROR: command not found: jq."
curl_helper GET "$dispatch_api_prefix" >/dev/null || die "cant request $dispatch_api_prefix, please run me in ci-dispatch host."
cmd="$1"
shift
func=cmd_$cmd
declare -F "$func" >/dev/null || die -e "invalid cmd: $cmd\n$usage"
$func "$@"
