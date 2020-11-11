#!/bin/bash

echo 'generate db_defect data...'

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"

mongodb="mongo ${MONGO_HOST}:${MONGO_PORT} -u ${MONGO_USER} -p ${MONGO_PASS} --authenticationDatabase admin"
$mongodb <<EOF1

use db_defect
db.createCollection("t_analysis_statistic")
db.createCollection("t_ccn_defect")
db.createCollection("t_ccn_statistic")
db.createCollection("t_checker_detail")
db.createCollection("t_checker_package")
db.createCollection("t_dupc_defect")
db.createCollection("t_dupc_statistic")
db.createCollection("t_filter_path")
db.createCollection("t_ignore_checker")
db.createCollection("t_lint_defect")
db.createCollection("t_lint_statistic")
db.createCollection("t_operation_history")
db.createCollection("t_red_line_meta")
db.createCollection("t_task_detail")
db.createCollection("t_task_log")
db.createCollection("t_transfer_author")
db.createUser({user:"bkcodecc",pwd:"codecc1",roles:[{role:"dbOwner",db:"db_defect"}]})

exit;
EOF1
