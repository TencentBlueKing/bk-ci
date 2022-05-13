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
REPO_INIT_GENERIC_METADATA_PATH="http://$BK_REPO_FQDN/generic/"
repo_generic_metdata_tmp_file=/tmp/bk_repo_generic_metdata_tmp_file.log
repo_generic_metdata_id_file=/tmp/bk_repo_generic_metdata_id_file.log

gen_repo_generic_metdata_tmp_file (){
    cat > $repo_generic_metdata_tmp_file << EOF
$(mysql --login-path=mysql-ci -Be "use devops_ci_artifactory;select C.ID,C.FILE_TYPE,C.PROJECT_CODE,C.FILE_PATH,C.UPDATE_TIME,B.PROPS_KEY,B.PROPS_VALUE FROM (select A.ID,A.PROJECT_CODE,A.FILE_TYPE,A.FILE_PATH,max(A.UPDATE_TIME) AS UPDATE_TIME from (select * from T_FILE_INFO) as A  group by A.PROJECT_CODE,A.FILE_TYPE order by A.UPDATE_TIME desc) C join T_FILE_PROPS_INFO  B on C.ID=B.FILE_ID order by C.PROJECT_CODE desc;"|grep -vw PROPS_KEY)
EOF
}

# 最终处理
gen_repo_generic_metdata_tmp_file || return $?
