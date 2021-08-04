#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

source ${CTRL_DIR:-/data/install}/load_env.sh
source ${CTRL_DIR:-/data/install}/bin/04-final/ci.env
cmd_mysql="mysql -h${BK_PAAS_MYSQL_HOST} -u${BK_PAAS_MYSQL_USER} -P $BK_PAAS_MYSQL_PORT open_paas"
export MYSQL_PWD=$BK_PAAS_MYSQL_PASSWORD

app_code=$BK_CI_APP_CODE
app_token=$BK_CI_APP_TOKEN
[ -z "$app_token" ] && { echo "无法获取app_token" ; exit 1; }
name="蓝盾"
introduction="持续集成平台（蓝盾）是一个免费并开源的CI服务，可助你自动化构建-测试-发布工作流，持续、快速、高质量地交付你的产品。"
name_en="bk-ci"
introduction_en="a free & open source CI server, bk-ci(BlueKing Continuous Integration) helps you automate your build-test-release workflows, continuous delivery of your product faster, easier, with fewer bugs."
creater="蓝鲸智云"
auth_token="$app_token"
logo="applogo/bk_ci.png"
external_url="$BK_CI_PUBLIC_URL"

echo "insert entry if not exist."
$cmd_mysql -e "select code from paas_app where code='$app_code';" | grep -q "$app_code" || {
    $cmd_mysql << EOF
INSERT INTO paas_app
(name,code,introduction,creater,state,is_already_test,is_already_online,first_test_time,first_online_time,language,auth_token,tags_id,deploy_token,is_use_celery,is_use_celery_beat,is_saas,logo,height,is_max,is_resize,is_setbar,use_count,width,external_url,is_default,is_sysapp,is_third,is_platform,is_lapp,is_display,open_mode,introduction_en,name_en,visiable_labels)
 VALUES (
"$name","$app_code","$introduction","$creater",4,TRUE ,TRUE,NULL,NOW(),"Java","$app_token",4,NULL,FALSE,FALSE,FALSE,"$logo",700,TRUE,TRUE,FALSE,0,1200,"$external_url",TRUE,FALSE,TRUE,TRUE,FALSE,TRUE,"desktop",introduction_en,name_en,"") ;
EOF
}

echo "then update entry."
$cmd_mysql << EOF
UPDATE paas_app SET
name="$name",introduction="$introduction",
name_en="$name_en",introduction_en="$introduction_en",
creater="$creater",first_online_time=NOW(),
auth_token="$auth_token",logo="$logo",
height=700,width=1200,external_url="$external_url"
WHERE code="$app_code";
EOF

echo "show entry:"
$cmd_mysql -e "select code,name,name_en,external_url from paas_app where code='$app_code';"
