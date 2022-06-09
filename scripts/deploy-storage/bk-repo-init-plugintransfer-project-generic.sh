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
REPO_QUERY_PROJECT_PATH="http://$BK_REPO_FQDN/repository/api/project/list"
REPO_QUERY_GENERIC_PATH="http://$BK_REPO_FQDN/repository/api/repo/info/"
REPO_CREATE_PROJECT_PATH="http://$BK_REPO_FQDN/api/repository/api/project"
REPO_CREATE_GENERIC_PATH="http://$BK_REPO_FQDN/repository/api/repo/create"

add_repo_domain (){
    if [[ -z $(grep $BK_REPO_FQDN /etc/hosts) ]] ; then
        echo "$BK_REPO_GATEWAY_IP0 $BK_REPO_FQDN" >> /etc/hosts
    fi
}

check_ci_project_name (){
    if [[ ! -z $(curl -s -X GET "$CI_QUERY_PROJECT_PATH"|grep projectName|awk -F\" '{print $(NF-1)}') ]] ; then
        echo "CI project query interface check success"
    else
        echo "CI project query interface check failed"
        exit 4
    fi
}

create_repo_project_name_init_plugintransfer_project_generic (){
    add_repo_domain || return $?
    check_ci_project_name || return $?
    for i in bk-store
    do
        ret=0
        echo "CI project is $i -------------------------------------------------->"
        # 蓝盾bk-store项目存在，Repo项目及项目的处理逻辑
        if [[ ! -z $(curl -s -XGET "$REPO_QUERY_PROJECT_PATH" -u admin:password|grep displayName|awk -F\" '{print $(NF-1)}'|grep $i) ]] ; then
            echo "$ret. Repo project $i exist , ${ret}st check success"
            for j in plugin static
            do
                ((ret++))
                if [[ ! -z $(curl -s -XGET "$REPO_QUERY_GENERIC_PATH"/$i/$j/generic -u admin:password|grep projectId) ]] ; then
                    echo "$ret. Repo project $i $j generic exist , ${ret}st check success"
                else
                    echo "$ret. Repo project $i $j generic not exist , 1st create it now ......"
                    curl -X POST "$REPO_CREATE_GENERIC_PATH" -u admin:password -H "X-BKREPO-UID: admin" -H "Content-Type: application/json" -d '{"projectId": "'$i'", "name": "'$j'", "type":"GENERIC", "category":"LOCAL", "public":false, "configuration": {"type":"local"}, "description":"storage for devops ci '$j'"}'
                    if [[ ! -z $(curl -s -XGET "$REPO_QUERY_GENERIC_PATH"/$i/$j/generic -u admin:password|grep projectId) ]] ; then
                        echo "$ret. Repo project $i $j generic ${ret}st create success"
                    else
                        echo "$ret. Repo project $i $j generic ${ret}st create failed"
                    fi
                
                fi
            done

        # 蓝盾bk-store项目不存在，Repo项目及仓库的处理逻辑
        else
            echo "$ret. Repo project $i not exist , ${ret}nd create $i project now ......"
            curl -X POST "$REPO_CREATE_PROJECT_PATH" -u admin:password -H "X-BKREPO-UID: admin" -H "Content-Type: application/json" -d '{"name": "'$i'", "displayName": "'$i'", "description": "'$i'"}'
            if [[ ! -z $(curl -s -XGET "$REPO_QUERY_PROJECT_PATH" -u admin:password|grep displayName|awk -F\" '{print $(NF-1)}'|grep $i) ]] ; then
                echo "$ret. Repo project $i create success , ${ret}nd check success"

                for j in plugin static
                do  
                    ((ret++))
                    curl -X POST "$REPO_CREATE_GENERIC_PATH" -u admin:password -H "X-BKREPO-UID: admin" -H "Content-Type: application/json" -d '{"projectId": "'$i'", "name": "'$j'", "type":"GENERIC", "category":"LOCAL", "public":false, "configuration": {"type":"local"}, "description":"storage for devops ci '$j'"}'
                    if [[ ! -z $(curl -s -XGET "$REPO_QUERY_GENERIC_PATH"/$i/$j/generic -u admin:password|grep projectId) ]] ; then
                        echo "$ret. Repo project $i $j generic ${ret}nd create success"
                    else
                        echo "$ret. Repo project $i $j generic ${ret}nd create failed"
                    fi
                done
            else
                echo "$ret. Repo project $i create failed , exit now" && exit 7 
            fi
        fi
        echo -e ""
    done
}

create_repo_project_name_init_plugintransfer_project_generic || return $?
