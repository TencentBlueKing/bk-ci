#!/bin/bash
# ci-turbo的op脚本.
# 目前仅适配了蓝鲸社区版的批量脚本.
# shellcheck disable=SC2059
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p "$0")."
}
# 检查bash版本.
if [ "${BASH_VERSINFO[0]:-0}" -lt 4 ]; then
  echo >&2 "ERROR: this script requires bash version greater than 4."
  exit 1
fi
tip (){ echo >&2 "$@";}
die (){ tip -e "$@"; exit 1;}
debug (){ [ -n "$DEBUG" ] && tip -e "\033[7mDEBUG\033[0m" "$@"; }

set -x 

ci_api_helper (){
  local method="$1"
  local uri="$2"
  local req="$3"
  curl -sSf -X "$method" -d "$req" \
    -H "Content-Type: application/json" \
    -H "X-DEVOPS-UID: admin" \
    "$ENDPOINT_CI_TURBO$uri"
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

# 参数验证
validate_scene (){
  grep -xqE "cc|ue4"
}
validate_by_regex (){
  local var="$1"
  local patt="$2"
  local tip="${3:-ERROR: $var(${!var:-EMPTY}) must match regex: $patt.}"
  if ! grep -xqE "$patt" <<< "${!var}"; then
    tip "$tip"
    return 1
  fi
}
patt_scene="cc|ue4"
#patt_project_id="[A-Za-z0-9_-]+"
#patt_worker_name="[A-Za-z0-9_.-]+"
#patt_worker_image="[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)+(:[A-Za-z0-9_.-]+)?"

patt_builder='[a-zA-Z0-9_.-]+'
validate_builder (){
  grep -xqE "$patt_builder"
}

#
fmt_ue4_download_url='<span class=\\"tip-word\\"><a class=\\"g-turbo-click-text\\" href=\\"%s\\" target=\\"__blank\\">UE%s</a></span>'
ue4_download_urls=$(
target="4.18 4.21 4.23 4.24 4.25 4.26"
for i in $target; do
  printf "$fmt_ue4_download_url" "$BK_TURBO_PUBLIC_URL/clients/bk-booster-for-ue$i.zip" "$i"
done
)
bk_turbo_install_cmd="curl -sSf ${BK_TURBO_PUBLIC_URL}/downloads/clients/install.sh | bash -s -- -r public"

# 添加编译加速引擎.
declare -A tpl_scene
tpl_scene["cc"]='{
    "engineCode" : "disttask-cc",
    "engineName" : "Linux-C/C++加速",
    "desc" : "全新自研引擎，零侵入一键加速，更快的速度，提供可视化编译图表，清晰掌控全流程数据。支持pch、gcov、分布式预处理等更多功能。",
    "spelExpression" : "(#end_time - #start_time) * ((#client_cpu == 0) ? 1 : (1 + 0.5 * ((#cpu_total / #client_cpu) ^ 0.5)))",
    "spelParamMap" : {
        "end_time" : 5,
        "start_time" : 2,
        "client_cpu" : 8,
        "cpu_total" : 16
    },
    "paramConfig" : [
        {
            "paramKey" : "queue_name",
            "paramName" : "资源池",
            "paramType" : "RADIO",
            "paramEnum" : [
                {
                    "paramValue" : "K8S://default",
                    "paramName" : "默认（Linux）",
                    "visualRange" : []
                }
            ],
            "displayed" : true,
            "defaultValue" : "K8S://default",
            "required" : true
        },
        {
            "paramKey" : "worker_version",
            "paramName" : "编译环境",
            "paramType" : "SELECT",
            "paramProps" : {
                "searchable" : true
            },
            "paramEnum" : [
              {
               "paramValue" : "tlinux2.4-gcc4.8.5",
               "paramName" : "tlinux2.4-gcc4.8.5",
               "visualRange" : []   
              } 
            ],
            "displayed" : true,
            "defaultValue" : "",
            "required" : true,
            "tips" : ""
        },
        {
            "paramKey" : "ccache_enabled",
            "paramName" : "是否启用ccache",
            "paramType" : "SWITCHER",
            "displayed" : true,
            "defaultValue" : true,
            "required" : false
        },
        {
            "paramKey" : "request_cpu",
            "paramName" : "cpu最佳需求",
            "paramType" : "INPUT",
            "displayed" : false,
            "defaultValue" : 144,
            "required" : false
        },
        {
            "paramKey" : "least_cpu",
            "paramName" : "cpu最低需求",
            "paramType" : "INPUT",
            "displayed" : false,
            "defaultValue" : 2,
            "required" : false
        }
    ],
    "enabled" : true,
    "recommend" : true,
    "recommendReason" : "全新自研引擎，零侵入一键加速，更快的速度，提供可视化编译图表，清晰掌控全流程数据。支持pch、gcov、分布式预处理等更多功能。",
    "pluginTips" : "示例：<br>\n<code style=\"background-color: #f1f1f1;padding: 2px;\">bk-booster -bt cc -p \"$TURBO_PLAN_ID\" --hook -a \"你的编译脚本或命令\"</code><br><br>\n注意：若你的编译环境是“私人构建机”，请预先在机器上用root执行安装脚本：<br>\n<code style=\"background-color: #f1f1f1;padding: 2px;\">'"$bk_turbo_install_cmd"'</code><br><br>\n更多详细指引请<a class=\"g-turbo-click-text\" href=\"https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/linux_c_cpp_local.md\" target=\"__blank\">参考文档</a></span>",
    "userManual" : "<h4>配置好加速方案后，有两种使用方式：</h4><br/>\n<h5>方式一：</h5><br/>\n<span class=\"tip-word\">在流水线中使用，添加【Turbo编译加速】插件，选择加速方案，配置加速脚本后执行流水线。<a class=\"g-turbo-click-text\" href=\"https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/linux_c_cpp.md\" target=\"__blank\">查看详细指引</a></span><br/>\n<h5>方式二：</h5><br/>\n<span class=\"tip-word\">在私人构建机上使用，以 root 权限执行安装脚本</span><code style=\"background-color: #f1f1f1;padding: 2px;\">'"$bk_turbo_install_cmd"'</code><br/>\n<span class=\"tip-word\">修改编译命令，配置加速脚本并执行。<a class=\"g-turbo-click-text\" href=\"https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/linux_c_cpp_local.md\" target=\"__blank\">查看详细指引</a></span>",
    "docUrl" : "https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/linux_c_cpp.md",
    "displayFields" : [
        {
            "fieldKey" : "queue_name",
            "fieldName" : "加速集群地区",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "request_cpu",
            "fieldName" : "加速资源",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "worker_version",
            "fieldName" : "worker版本",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "client_version",
            "fieldName" : "client版本",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "client_ip",
            "fieldName" : "发起机器ip",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "ccache_enabled",
            "fieldName" : "CCache",
            "link" : false,
            "linkTemplate" : ""
        }
    ]
}'

tpl_scene["ue4"]='{
    "priorityNum" : 2,
    "engineCode" : "disttask-ue4",
    "engineName" : "UE4加速",
    "desc" : "自研引擎，多平台下的 UE4 构建加速。支持 Editor、Shader、Dedicated Servers 等加速。",
    "spelExpression" : "(#end_time - #start_time) * ((#client_cpu == 0) ? 1 : (1 + 0.5 * ((#cpu_total / #client_cpu) ^ 0.5)))",
    "spelParamMap" : {
        "end_time" : 5,
        "start_time" : 2,
        "client_cpu" : 8,
        "cpu_total" : 16
    },
    "paramConfig" : [
        {
            "paramKey" : "queue_name",
            "paramName" : "资源池",
            "paramType" : "RADIO",
            "paramEnum" : [
                {
                    "paramValue" : "K8S_WIN://default",
                    "paramName" : "默认（Windows）",
                    "visualRange" : []
                }
            ],
            "displayed" : true,
            "defaultValue" : "K8S_WIN://default",
            "required" : true
        },
        {
            "paramKey" : "worker_version",
            "paramName" : "编译环境",
            "paramType" : "SELECT",
            "paramProps" : {
                "searchable" : true
            },
            "paramEnum" : [
                {
                    "paramValue" : "win2019-pure-drived",
                    "paramName" : "Windows通用环境",
                    "visualRange" : []
                }
            ],
            "displayed" : true,
            "defaultValue" : "",
            "required" : true,
            "tips" : ""
        },
        {
            "paramKey" : "request_cpu",
            "paramName" : "cpu最佳需求",
            "paramType" : "INPUT",
            "displayed" : false,
            "defaultValue" : 144,
            "required" : false
        },
        {
            "paramKey" : "least_cpu",
            "paramName" : "cpu至少限制",
            "paramType" : "INPUT",
            "displayed" : false,
            "defaultValue" : 144,
            "required" : false
        }
    ],
    "recommend" : true,
    "recommendReason" : "自研引擎，多平台下的 UE4 构建加速。支持 Editor、Shader、Dedicated Servers 等编译加速。",
    "pluginTips" : "UE4方案的接入办法详见<a class=\"g-turbo-click-text\" href=\"https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/ue.md\" target=\"__blank\">接入文档</a>",
    "userManual" : "<h3>配置好加速方案后，根据指引来手动接入：</h3><br/> <span class=\"tip-word\"><a class=\"g-turbo-click-text\" href=\"https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/ue.md\" target=\"__blank\">查看接入指引</a></span><br/> <h4>下载工具包：</h4><br/> <h5>Windows平台</h5><br/>'"$ue4_download_urls"'",
    "docUrl" : "https://bk.tencent.com/docs/markdown/编译加速/产品白皮书/Quickstart/ue.md",
    "displayFields" : [
        {
            "fieldKey" : "queue_name",
            "fieldName" : "加速集群地区",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "request_cpu",
            "fieldName" : "加速资源",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "worker_version",
            "fieldName" : "worker版本",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "client_version",
            "fieldName" : "client版本",
            "link" : false,
            "linkTemplate" : ""
        },
        {
            "fieldKey" : "client_ip",
            "fieldName" : "发起机器ip",
            "link" : false,
            "linkTemplate" : ""
        }
    ]
}'

# ci-turbo
scene_list (){
  local api_url="/api/op/turboEngineConfig/all"
  ci_api_helper "GET" "$api_url" "" | jq -r '.data[] | [.engineCode,.engineName,.enabled] | @tsv' | 
    pretty_tsv
}
scene_add (){
  local usage="scene add SCENE"
  local desc="add a new SCENE in ci-turbo."
  local scene="${1:-}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  local api_url="/api/op/turboEngineConfig/"
  local req="${tpl_scene[$scene]}"
  ci_api_helper "POST" "$api_url" "$req" 
  sleep 1
}
scene_init (){
  local usage="scene init SCENE"
  local desc="reset SCENE data to init version, all existing builders would be removed."
  local scene="${1:-}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  local api_url="/api/op/turboEngineConfig/engineCode/disttask-$scene"
  local req="${tpl_scene[$scene]}"
  ci_api_helper "PUT" "$api_url" "$req" | jq .
}
scene_del (){
  local usage="scene del SCENE"
  local desc="delete SCENE"
  local scene="${1:-}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  local api_url="/api/op/turboEngineConfig/engineCode/disttask-$scene"
  local req=''
  ci_api_helper "DELETE" "$api_url" "$req" | jq .
}
scene_get (){
  local usage="scene get SCENE"
  local desc="show SCENE details."
  local scene="${1:-cc}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  local api_url="/api/op/turboEngineConfig/engineCode/disttask-$scene"
  ci_api_helper "GET" "$api_url" "" | jq .
}
# ci-turbo
fmt_builder_ci='{"paramName":"%s","paramValue": "%s"}'
builder_attach (){
  local usage="builder attach SCENE WORKER [NAME=WORKER]"
  local desc="attach tbs WORKER to ci. if NAME is empty, using WORKER instead."
  local scene="${1:-}"
  local builder="${2:-}"
  local builder_display_name="${3:-$builder}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  validate_builder <<< "$builder" || die "WORKER should match regex $patt_builder. \nUsage: $usage -- $desc"
  local api_url="/api/op/turboEngineConfig/workVersion/engineCode/disttask-$scene"
  local req=''
  printf -v req "$fmt_builder_ci" "$builder_display_name" "$builder"
  ci_api_helper "POST" "$api_url" "$req" | jq .
}
builder_detach (){
  local usage="builder detach SCENE WORKER"
  local desc="detach tbs WORKER from ci."
  local scene="${1:-}"
  local builder="${2:-}"
  validate_by_regex scene "$patt_scene" || die "Usage: $usage"
  validate_builder <<< "$builder" || die "WORKER should match regex $patt_builder. \nUsage: $usage -- $desc"
  local api_url="/api/op/turboEngineConfig/workVersion/engineCode/disttask-$scene/paramValue/$builder"
  local req=''
  ci_api_helper "DELETE" "$api_url" "$req" | jq .
}

target="${1:-}"
action="${2:-}"
if [ -z "$target" ] || [ -z "$action" ]; then
  cat >&2 <<EOF
Usage: $0 CMD [ARGS]  -- Blueking Turbo op tools.
CMD:
  scene list    show all scenes in ci.
  scene add     add a scene for ci.
  scene get     show detail for a scene, include attached builders.
  scene set     update scene config fields.
  scene del     delete a scene. all builders were removed.
  scene init    reset scene config to default value. all builders were removed.
  builder attach   attach builder to scene in ci.
  builder detach   detach builder to scene in ci.
EOF
  exit 1
fi
cmd="${target}_${action}"
shift 2
$cmd "$@"
