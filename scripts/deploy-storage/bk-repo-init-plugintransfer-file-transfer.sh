#!/bin/bash

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

#if [ $# -lt 1 ]; then
#    echo "Usage: $0 MS_NAME [OPTIONS-for-MS_NAME]"
#    exit 1
#fi

MS_USER=${MS_USER:-blueking}  # 暂不建议修改为其他用户, 此功能未测试.
BK_PKG_SRC_PATH=${BK_REPO_SRC_DIR:-/data/src}
BK_REPO_SRC_DIR="${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}"  # repo安装源
CTRL_DIR=${CTRL_DIR:-/data/install}
LAN_IP=${LAN_IP:-$(ip route show | grep -Pom 1 "(?<=src )[0-9.]+")}
source ${CTRL_DIR:-/data/install}/load_env.sh
CI_QUERY_PROJECT_PATH="http://$BK_CI_GATEWAY_IP0:80/project/api/service/projects/getAllProject"
REPO_INIT_GENERIC_METADATA_PATH="http://$BK_REPO_FQDN/generic/"

process_artifactory_plugintransfer_file_transfer_repo (){
    # 路径1处理
    o_j="$BK_CI_DATA_DIR/artifactory/bk-atom/"
    for i in $(find ${o_j} -maxdepth 5 -type f)
    do
        curl -s -X PUT $REPO_INIT_GENERIC_METADATA_PATH/bk-store/plugin/$(echo ${i}|sed "s#${o_j}##g") -u admin:password -H \"X-BKREPO-UID: admin\" -H \"X-BKREPO-OVERWRITE: true\" --upload-file ${i}
        echo -e ""
    done

    # 路径2处理
    p_j="$BK_CI_DATA_DIR/artifactory/static/"
    for j in $(find ${p_j} -maxdepth 5 -type f)
    do
        curl -s -X PUT $REPO_INIT_GENERIC_METADATA_PATH/bk-store/static/$(echo $j|sed "s#${p_j}##g"|awk -F"/" '{$1=null;print $0}'|sed "s# #/#g") -u admin:password -H "X-BKREPO-UID: admin" -H "X-BKREPO-OVERWRITE: true" --upload-file ${j}
        echo -e ""
    done

    # 路径3处理
    q_j="$BK_CI_DATA_DIR/artifactory/file/png/"
    for k in $(find ${q_j} -maxdepth 1 -type f)
    do
        curl -s -X PUT $REPO_INIT_GENERIC_METADATA_PATH/bk-store/static/$(echo $k|sed "s#${BK_CI_DATA_DIR}/artifactory/##g") -u admin:password -H \"X-BKREPO-UID: admin\" -H \"X-BKREPO-OVERWRITE: true\" --upload-file ${k}
        echo -e ""
    done

}

# 最终处理
process_artifactory_plugintransfer_file_transfer_repo || return $?
