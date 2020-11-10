#!/bin/bash

echo 'generate db_schedule data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"
echo 'start create schedule ddl...'

mongodb="mongo ${MONGO_HOST}:${MONGO_PORT} -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin"
$mongodb <<EOF1

use db_schedule
db.createCollection("t_analyze_host")
db.createCollection("t_file_index")
db.createUser({user:"bkcodecc",pwd:"codecc1",roles:[{role:"dbOwner",db:"db_schedule"}]})

exit;
EOF1
