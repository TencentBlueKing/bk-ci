#!/bin/bash

##### 生成bk环境变量#####
[ -d "$CTRL_DIR" ] || { echo "please run me in blueking node."; exit 1; }
OPWD=$PWD
source "$CTRL_DIR/utils.fc"
cd "$OPWD"

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"

mongodb="mongo ${MONGODB_IP}:${MONGODB_PORT} -u ${MONGODB_USER} -p ${MONGODB_PASS}"
$mongodb <<EOF

# 新建表
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

# 索引
#db.getCollection('t_analysis_statistic').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_ccn_defect').createIndex({'task_id':1})
#db.getCollection(t_ccn_defect').createIndex({'rel_path':1})
#db.getCollection('t_ccn_defect').createIndex({'status':1})
#db.getCollection('t_ccn_statistic').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_checker_detail').createIndex({'task_id':1})
#db.getCollection('t_checker_detail').createIndex({'status':1})
#db.getCollection('t_checker_package').createIndex({'tool_name':1})
#db.getCollection('t_dupc_defect').createIndex({'task_id':1})
#db.getCollection('t_dupc_defect').createIndex({'status':1})
#db.getCollection('t_dupc_defect').createIndex({'rel_path':1})
#db.getCollection('t_dupc_statistic').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_ignore_checker').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_lint_defect').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_lint_defect').createIndex({'rel_path':1})
#db.getCollection('t_lint_defect').createIndex({'status':1})
#db.getCollection('t_lint_statistic').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_operation_history').createIndex({'task_id':1})
#db.getCollection('t_operation_history').createIndex({'func_id':1})
#db.getCollection('t_task_log').createIndex({'task_id':1,'tool_name':1})
#db.getCollection('t_task_log').createIndex({'stream_name':1})
#db.getCollection('t_transfer_author').createIndex({'task_id':1,'tool_name':1})

exit;
EOF1
