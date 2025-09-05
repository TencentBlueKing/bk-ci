USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'INSTANCE_ERROR_INFO') THEN
    ALTER TABLE T_TEMPLATE_PIPELINE
        ADD COLUMN `INSTANCE_ERROR_INFO` text null comment '实例化错误信息';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                     AND INDEX_NAME = 'INX_PIPELINE_UPDATE_TIME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD INDEX `INX_PIPELINE_UPDATE_TIME`(`PROJECT_ID`,`PIPELINE_ID`,`UPDATE_TIME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'LOCKED') THEN
    ALTER TABLE T_PIPELINE_INFO
        ADD COLUMN `LOCKED` bit(1) DEFAULT b'0' COMMENT '是否锁定，PAC v3.0新增锁定，取代原来setting表中的LOCK';
    END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'JOB_ID') THEN
        ALTER TABLE `T_PIPELINE_BUILD_TASK`
			ADD COLUMN `JOB_ID` varchar(128) NULL COMMENT 'job id';
    END IF;


	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_CONTAINER'
                        AND COLUMN_NAME = 'JOB_ID') THEN
        ALTER TABLE `T_PIPELINE_BUILD_CONTAINER`
			ADD COLUMN `JOB_ID` varchar(128) NULL COMMENT 'job id';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_RECORD_TASK'
                    AND COLUMN_NAME = 'ASYNC_STATUS') THEN
    ALTER TABLE `T_PIPELINE_BUILD_RECORD_TASK`
        ADD COLUMN `ASYNC_STATUS` varchar(32) DEFAULT NULL COMMENT '插件异步执行状态';
    END IF;

    COMMIT;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'RELEASE_TIME') THEN
    ALTER TABLE T_PIPELINE_RESOURCE_VERSION
        ADD COLUMN RELEASE_TIME TIMESTAMP NULL COMMENT '发布时间';
    END IF;

	IF NOT EXISTS(SELECT 1
	                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                        AND COLUMN_NAME = 'STAGE_ID_FOR_USER') THEN
    ALTER TABLE `T_PIPELINE_BUILD_STAGE`
			ADD COLUMN `STAGE_ID_FOR_USER` varchar(64) DEFAULT NULL COMMENT '当前stageId 阶段ID (用户可编辑)';
    END IF;

	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'MAX_CON_RUNNING_QUEUE_SIZE') THEN
    ALTER TABLE T_PIPELINE_SETTING_VERSION
        ADD COLUMN `MAX_CON_RUNNING_QUEUE_SIZE` int(11) DEFAULT NULL COMMENT '并发构建数量限制,值为-1时表示取系统默认值。';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TRIGGER_EVENT'
                    AND COLUMN_NAME = 'EVENT_BODY') THEN
        ALTER TABLE `T_PIPELINE_TRIGGER_EVENT`
            ADD COLUMN `EVENT_BODY` longtext NULL COMMENT '事件体';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_YAML_BRANCH_FILE'
                    AND COLUMN_NAME = 'UPDATE_TIME') THEN
        ALTER TABLE `T_PIPELINE_YAML_BRANCH_FILE`
            ADD COLUMN `UPDATE_TIME`  datetime not null  default CURRENT_TIMESTAMP comment '更新时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_YAML_BRANCH_FILE'
                    AND COLUMN_NAME = 'COMMIT_ID') THEN
        ALTER TABLE `T_PIPELINE_YAML_BRANCH_FILE`
            ADD COLUMN `COMMIT_ID` varchar(64) null comment '文件commitId';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_YAML_BRANCH_FILE'
                    AND COLUMN_NAME = 'BLOB_ID') THEN
        ALTER TABLE `T_PIPELINE_YAML_BRANCH_FILE`
            ADD COLUMN `BLOB_ID`   varchar(64) not null comment '文件blob_id';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_YAML_BRANCH_FILE'
                    AND COLUMN_NAME = 'COMMIT_TIME') THEN
        ALTER TABLE `T_PIPELINE_YAML_BRANCH_FILE`
            ADD COLUMN `COMMIT_TIME`  datetime not null default CURRENT_TIMESTAMP not null comment '提交时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_YAML_BRANCH_FILE'
                    AND COLUMN_NAME = 'DELETED') THEN
        ALTER TABLE `T_PIPELINE_YAML_BRANCH_FILE`
            ADD COLUMN `DELETED` bit not null default b'0' comment '是否删除';
    END IF;


  IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_TIMER'
                AND INDEX_NAME = 'IDX_PIPELINE_ID') THEN
        ALTER TABLE T_PIPELINE_TIMER DROP INDEX `IDX_PIPELINE_ID`;
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_PIPELINE_TIMER'
                  AND COLUMN_NAME = 'TASK_ID') THEN
    ALTER TABLE `T_PIPELINE_TIMER`
      ADD COLUMN `TASK_ID` varchar(64) DEFAULT '' COMMENT '插件ID' AFTER PIPELINE_ID,
        DROP PRIMARY KEY, ADD PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `TASK_ID`);
    END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_PIPELINE_TIMER'
                  AND COLUMN_NAME = 'START_PARAM') THEN
  ALTER TABLE `T_PIPELINE_TIMER`
      ADD COLUMN `START_PARAM` text NULL COMMENT '插件启动参数';
  END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TIMER_BRANCH'
                    AND COLUMN_NAME = 'TASK_ID') THEN
  ALTER TABLE `T_PIPELINE_TIMER_BRANCH`
      ADD COLUMN `TASK_ID` varchar(64) DEFAULT '' COMMENT '插件ID' AFTER PIPELINE_ID,
  DROP PRIMARY KEY, ADD PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `TASK_ID`, `REPO_HASH_ID`, `BRANCH`);
  END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'FAIL_IF_VARIABLE_INVALID') THEN
        ALTER TABLE `T_PIPELINE_SETTING`
            ADD COLUMN `FAIL_IF_VARIABLE_INVALID` bit default null comment '是否配置流水线变量值超长时终止执行';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'FAIL_IF_VARIABLE_INVALID') THEN
        ALTER TABLE `T_PIPELINE_SETTING_VERSION`
            ADD COLUMN `FAIL_IF_VARIABLE_INVALID` bit default null comment '是否配置流水线变量值超长时终止执行';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND COLUMN_NAME = 'NAME') THEN
    ALTER TABLE T_PIPELINE_VIEW MODIFY COLUMN NAME varchar(255) NOT NULL COMMENT '名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                AND COLUMN_NAME = 'ARTIFACT_QUALITY_INFO') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
        ADD COLUMN `ARTIFACT_QUALITY_INFO` mediumtext CHARACTER SET utf8mb4 comment '制品质量分析结果' after `ARTIFACT_INFO`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                AND COLUMN_NAME = 'ARTIFACT_QUALITY_INFO') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY_DEBUG`
        ADD COLUMN `ARTIFACT_QUALITY_INFO` mediumtext CHARACTER SET utf8mb4 comment '制品质量分析结果' after `ARTIFACT_INFO`;
    END IF;

COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
