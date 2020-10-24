#!/bin/bash

##### 打印当时执行脚本的时间戳及PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"

db_user=${MONGODB_USER}
db_pwd=${MONGODB_PASS}
task_data_path=/data/bkee/codecc/support-files/nosql/db_task_data
defect_data_path=/data/bkee/codecc/support-files/nosql/db_defect_data

# db_task生成表
sh ${task_data_path}/db_task_ddl.sh

# db_defect生成表
sh ${defect_data_path}/db_defect_ddl.sh
 
# 导入db_task基础数据
mongoimport -d db_task -u ${db_user} -p ${db_pwd} -c t_base_data --file=${task_data_path}/t_base_data.json
mongoimport -d db_task -u ${db_user} -p ${db_pwd} -c t_tool_meta --file=${task_data_path}/t_tool_meta.json

# 导入db_defect基础数
mongoimport -d db_defect -u ${db_user} -p ${db_pwd} -c t_checker_package --file=${defect_data_path}/t_checker_package.json
mongoimport -d db_defect -u ${db_user} -p ${db_pwd} -c t_checker_detail --file=${defect_data_path}/t_checker_detail.json
mongoimport -d db_defect -u ${db_user} -p ${db_pwd} -c t_red_line_meta --file=${defect_data_path}/t_red_line_meta.json
