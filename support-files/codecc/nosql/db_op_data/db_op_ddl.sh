#!/bin/bash

echo 'generate db_op data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"
echo 'start create op ddl...'

mongodb="mongo ${BK_CODECC_MONGODB_HOST}:${BK_CODECC_MONGODB_PORT} -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} --authenticationDatabase admin"
$mongodb <<EOF1

use db_op
db.createCollection("t_appcode_admin")
db.createCollection("t_appcode_org")
db.createCollection("t_appcode_project")
db.createCollection("t_appcode_tool")

exit;
EOF1
