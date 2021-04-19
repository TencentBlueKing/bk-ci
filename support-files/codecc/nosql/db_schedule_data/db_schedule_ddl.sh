#!/bin/bash

echo 'generate db_schedule data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"
echo 'start create schedule ddl...'

mongodb="mongo ${BK_CODECC_MONGODB_HOST}:${BK_CODECC_MONGODB_PORT} -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} --authenticationDatabase admin"
$mongodb <<EOF1

use db_schedule
db.createCollection("t_analyze_host")
db.createCollection("t_file_index")

exit;
EOF1