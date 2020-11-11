#!/bin/bash

echo 'generate db_task data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"
echo 'start create task ddl...'

mongodb="mongo ${MONGO_HOST}:${MONGO_PORT} -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin"
$mongodb <<EOF1

use db_task
db.createCollection("t_base_data")
db.createCollection("t_filter_path")
db.createCollection("t_task_detail")
db.createCollection("t_tool_config")
db.createCollection("t_tool_meta")
db.createUser({user:"bkcodecc",pwd:"codecc1",roles:[{role:"dbOwner",db:"db_task"}]})

exit;
EOF1
