#!/bin/bash
source ${CTRL_DIR:-/data/install}/load_env.sh
cmd_mysql="mysql -h${BK_PAAS_MYSQL_HOST} -u${BK_PAAS_MYSQL_USER} -P $BK_PAAS_MYSQL_PORT open_paas"
export MYSQL_PWD=$BK_PAAS_MYSQL_PASSWORD

app_code=$BK_CI_APP_CODE
app_token=$BK_CI_APP_TOKEN
[ -z "$app_token" ] && { echo "无法获取app_token" ; exit 1; }

echo "delete entry if exist."
$cmd_mysql -ve "delete from paas_app where code='$app_code';"
echo "mysql command returns $?."

