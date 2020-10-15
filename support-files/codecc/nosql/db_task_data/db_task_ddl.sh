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
echo 'start create task ddl...'

mongodb="mongo ${MONGODB_IP}:${MONGODB_PORT} -u ${MONGODB_USER} -p ${MONGODB_PASS}"
$mongodb <<EOF

# 新建表
use db_task
db.createCollection("t_base_data")
db.createCollection("t_filter_path")
db.createCollection("t_task_detail")
db.createCollection("t_tool_config")
db.createCollection("t_tool_meta")

# 索引
#db.getCollection('t_base_data').createIndex({'param_code':1})
#db.getCollection('t_task_detail').createIndex({'task_id':1})
#db.getCollection('t_task_detail').createIndex({'project_id':1})
#db.getCollection('t_task_detail').createIndex({'status':1})
#db.getCollection('t_tool_config').createIndex({'task_id':1})
#db.getCollection('t_tool_config').createIndex({'tool_name':1})
#db.getCollection('t_tool_meta').createIndex({'name':1})

exit;
EOF1
