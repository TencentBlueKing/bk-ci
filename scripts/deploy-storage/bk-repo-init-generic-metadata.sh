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
REPO_INIT_GENERIC_METADATA_PATH="http://$BK_REPO_FQDN/generic"
repo_generic_metdata_tmp_file=/tmp/bk_repo_generic_metdata_tmp_file.log

gen_repo_generic_metdata_tmp_file (){
if [[ ! -f $repo_generic_metdata_tmp_file ]] ; then
    cat > $repo_generic_metdata_tmp_file << EOF
$(mysql --login-path=mysql-ci -Be "use devops_ci_artifactory;select C.ID,C.FILE_TYPE,C.PROJECT_CODE,C.FILE_PATH,C.UPDATE_TIME,B.PROPS_KEY,B.PROPS_VALUE FROM (select A.ID,A.PROJECT_CODE,A.FILE_TYPE,A.FILE_PATH,max(A.UPDATE_TIME) AS UPDATE_TIME from (select * from T_FILE_INFO) as A  group by A.PROJECT_CODE,A.FILE_TYPE order by A.UPDATE_TIME desc) C join T_FILE_PROPS_INFO  B on C.ID=B.FILE_ID order by C.PROJECT_CODE desc;"|grep -vw PROPS_KEY)
EOF
fi
}

process_ci_artifactory_process_generic_metdata (){
    # 查询出所有的待上传文件及元数据
    #gen_repo_generic_metdata_tmp_file || return $?
    while read line ; do
        file_generic_type=$(grep $line $repo_generic_metdata_tmp_file|awk '{print $2}'|sort -u)
        file_project_code=$(grep $line $repo_generic_metdata_tmp_file|awk '{print $3}'|sort -u)
        shaContent=$(grep $line $repo_generic_metdata_tmp_file|grep shaContent|awk '{print $NF}')
        key=$(echo $(grep $line $repo_generic_metdata_tmp_file|awk '{print $(NF-1)"="$NF}'|tr '\n' '&'|sed 's/.$//g')|base64|sed ":a;N;s/\n//g;ta")
        if [[ $(grep $line $repo_generic_metdata_tmp_file|awk '{print $2}'|sort -u) == bk-custom ]] ; then
            generic_type=custom
        elif [[ $(grep $line $repo_generic_metdata_tmp_file|awk '{print $2}'|sort -u) == bk-archive ]] ; then
            generic_type=pipeline
        else
            generic_type=report
        fi
        pre_upload_file_name=$(grep $line $repo_generic_metdata_tmp_file|awk '{print $4}'|sort -u|sed -e "s/$file_generic_type\/$file_project_code\///g")
        ci_artifactory_file_path_name="$BK_CI_DATA_DIR/artifactory/$(grep $line $repo_generic_metdata_tmp_file|awk '{print $4}'|sort -u)"
        echo $file_project_code $generic_type $pre_upload_file_name $ci_artifactory_file_path_name $(grep $line $repo_generic_metdata_tmp_file|awk '{print $(NF-1)"="$NF}'|tr '\n' '&'|sed 's/.$//g')
        echo "curl -s -X PUT ${REPO_INIT_GENERIC_METADATA_PATH}/${file_project_code}/${generic_type}/${pre_upload_file_name} -u admin:password -H \"X-BKREPO-UID: admin\" -H \"X-BKREPO-OVERWRITE: true\" -H \"X-BKREPO-META: ${key}\" --upload-file $ci_artifactory_file_path_name"
        if [[ $(curl -s -X PUT ${REPO_INIT_GENERIC_METADATA_PATH}/${file_project_code}/${generic_type}/${pre_upload_file_name} -u admin:password -H "X-BKREPO-UID: admin" -H "X-BKREPO-OVERWRITE: true" -H "X-BKREPO-META: ${key}" --upload-file $ci_artifactory_file_path_name|grep -w shaContent|awk -F\" '{print $(NF-1)}'|sort -u) == $shaContent ]] ; then
            echo "$file_project_code $generic_type $pre_upload_file_name upload and metadata init success"
        else
            echo "$file_project_code $generic_type $pre_upload_file_name upload and metadata init failed"
            exit 9
        fi
        echo -e ""
    done < <(sort -k3 $repo_generic_metdata_tmp_file|awk '{print $1}'|awk '!x[$0]++')
}

# 最终处理
process_ci_artifactory_process_generic_metdata || return $?
