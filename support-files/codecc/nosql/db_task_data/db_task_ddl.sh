#!/bin/bash

echo 'generate db_task data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"
echo 'start create task ddl...'

mongodb="mongo ${BK_CODECC_MONGODB_HOST}:${BK_CODECC_MONGODB_PORT} -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} --authenticationDatabase admin"
$mongodb <<EOF1

use db_task
db.createCollection("t_base_data")
db.createCollection("t_filter_path")
db.createCollection("t_task_detail")
db.createCollection("t_tool_config")
db.createCollection("t_tool_meta")

exit;
EOF1
