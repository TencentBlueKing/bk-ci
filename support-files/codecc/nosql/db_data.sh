#!/bin/bash

##### print current timestamp and PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"


task_data_path=support-files/nosql/db_task_data
defect_data_path=support-files/nosql/db_defect_data
schedule_data_path=support-files/nosql/db_schedule_data
op_data_path=support-files/nosql/db_op_data
quartz_data_path=support-files/nosql/db_quartz_data

echo 'current execute path is:'`pwd`
echo 'generate db_task table'
sh ${task_data_path}/db_task_ddl.sh

echo 'generate db_defect table'
sh ${defect_data_path}/db_defect_ddl.sh

echo 'generate db schedule table'
sh ${schedule_data_path}/db_schedule_ddl.sh

echo 'generate db op table'
sh ${op_data_path}/db_op_ddl.sh

echo 'generate db quartz table'
sh ${quartz_data_path}/db_quartz_ddl.sh

echo 'start to import db task data'
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_task -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_base_data --file=${task_data_path}/0001_codecc_db_task_t_base_data_mongo.json --authenticationDatabase admin
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_task -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_tool_meta --file=${task_data_path}/0002_codecc_db_task_t_tool_meta_mongo.json --authenticationDatabase admin

echo 'start to import db defect data'
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_defect -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_checker_package --file=${defect_data_path}/0004_codecc_db_defect_t_checker_package_mongo.json --authenticationDatabase admin
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_defect -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_checker_set --file=${defect_data_path}/0002_codecc_db_defect_t_checker_set_mongo.json --authenticationDatabase admin
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_defect -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_checker_detail --file=${defect_data_path}/0001_codecc_db_defect_t_checker_detail_mongo.json --authenticationDatabase admin
mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -d db_defect -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} -c t_red_line_meta --file=${defect_data_path}/0003_codecc_db_defect_t_red_line_meta_mongo.json --authenticationDatabase admin

