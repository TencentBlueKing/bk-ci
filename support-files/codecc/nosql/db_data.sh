#!/bin/bash

##### print current timestamp and PID。
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
echo "`eval $NOW` job_start"


task_data_path=/data/docker/bkci/codecc/configuration/support-files/nosql/db_task_data
defect_data_path=/data/docker/bkci/codecc/configuration/support-files/nosql/db_defect_data
schedule_data_path=/data/docker/bkci/codecc/configuration/support-files/nosql/db_schedule_data

echo 'current execute path is:'`pwd`
echo 'generate db_task table'
sh ${task_data_path}/db_task_ddl.sh

echo 'generate db_defect table'
sh ${defect_data_path}/db_defect_ddl.sh

echo 'generate db schedule table'
sh ${schedule_data_path}/db_schedule_ddl.sh

echo 'start to import db task data'
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_task -u ${MONGO_USER} -p ${MONGO_PASS} -c t_base_data --file=${task_data_path}/0001_db_task_t_base_data_20201013-1010_mongo.json --authenticationDatabase admin
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_task -u ${MONGO_USER} -p ${MONGO_PASS} -c t_tool_meta --file=${task_data_path}/0002_db_task_t_tool_meta_20201013-1010_mongo.json --authenticationDatabase admin

echo 'start to import db defect data'
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_defect -u ${MONGO_USER} -p ${MONGO_PASS} -c t_checker_package --file=${defect_data_path}/0004_db_defect_t_checker_package_20201013-1010_mongo.json --authenticationDatabase admin
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_defect -u ${MONGO_USER} -p ${MONGO_PASS} -c t_checker_set --file=${defect_data_path}/0002_db_defect_t_checker_set_20201013-1010_mongo.json --authenticationDatabase admin
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_defect -u ${MONGO_USER} -p ${MONGO_PASS} -c t_checker_detail --file=${defect_data_path}/0001_db_defect_t_checker_detail_20201013-1010_mongo.json --authenticationDatabase admin
mongoimport --host ${MONGO_HOST} --port ${MONGO_PORT} -d db_defect -u ${MONGO_USER} -p ${MONGO_PASS} -c t_red_line_meta --file=${defect_data_path}/0003_db_defect_t_red_line_meta_20201013-1010_mongo.json --authenticationDatabase admin

