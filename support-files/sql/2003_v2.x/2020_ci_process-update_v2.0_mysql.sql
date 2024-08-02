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
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_RECORD_TASK'
                    AND COLUMN_NAME = 'POST_INFO') THEN
    ALTER TABLE `T_PIPELINE_BUILD_RECORD_TASK`
        ADD COLUMN `POST_INFO` text DEFAULT NULL COMMENT '市场插件的POST关联信息';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_PAUSE_VALUE'
                    AND COLUMN_NAME = 'EXECUTE_COUNT') THEN
    ALTER TABLE `T_PIPELINE_PAUSE_VALUE`
        ADD COLUMN `EXECUTE_COUNT` int(11) DEFAULT NULL COMMENT '执行次数';
    ALTER TABLE `T_PIPELINE_PAUSE_VALUE` DROP PRIMARY KEY;
    ALTER TABLE `T_PIPELINE_PAUSE_VALUE`
        ADD CONSTRAINT TASK_EXECUTE_COUNT UNIQUE (`PROJECT_ID`,`BUILD_ID`,`TASK_ID`,`EXECUTE_COUNT`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_AUDIT_RESOURCE'
                     AND INDEX_NAME = 'IDX_TAR_USER_ID') THEN
    ALTER TABLE `T_AUDIT_RESOURCE`
        ADD INDEX `IDX_TAR_USER_ID`(`PROJECT_ID`,`RESOURCE_TYPE`,`USER_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_AUDIT_RESOURCE'
                     AND INDEX_NAME = 'IDX_TAR_RESOURCE_ID') THEN
    ALTER TABLE `T_AUDIT_RESOURCE`
        ADD INDEX `IDX_TAR_RESOURCE_ID`(`PROJECT_ID`,`RESOURCE_TYPE`,`RESOURCE_ID`);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_AUDIT_RESOURCE'
                   AND INDEX_NAME = 'IDX_SEARCH') THEN
    ALTER TABLE `T_AUDIT_RESOURCE` DROP INDEX `IDX_SEARCH`;
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_AUDIT_RESOURCE'
                   AND INDEX_NAME = 'IDX_SEARCH_ID') THEN
    ALTER TABLE `T_AUDIT_RESOURCE` DROP INDEX `IDX_SEARCH_ID`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PIPELINE_VIEW_USER_LAST_VIEW'
                     AND INDEX_NAME = 'IDX_TPVULV_PROJECT_ID') THEN
    ALTER TABLE `T_PIPELINE_VIEW_USER_LAST_VIEW`
        ADD INDEX `IDX_TPVULV_PROJECT_ID`(`PROJECT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PIPELINE_VIEW_USER_SETTINGS'
                     AND INDEX_NAME = 'IDX_TPVUS_PROJECT_ID') THEN
    ALTER TABLE `T_PIPELINE_VIEW_USER_SETTINGS`
        ADD INDEX `IDX_TPVUS_PROJECT_ID`(`PROJECT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND COLUMN_NAME = 'EVENT_TYPE') THEN
    ALTER TABLE `T_PIPELINE_WEBHOOK`
        ADD COLUMN `EVENT_TYPE` varchar(64) DEFAULT null COMMENT '事件类型';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND COLUMN_NAME = 'EXTERNAL_ID') THEN
    ALTER TABLE `T_PIPELINE_WEBHOOK`
        ADD COLUMN `EXTERNAL_ID` varchar(255) DEFAULT null COMMENT '代码库平台ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND COLUMN_NAME = 'REPOSITORY_HASH_ID') THEN
    ALTER TABLE `T_PIPELINE_WEBHOOK`
        ADD COLUMN `REPOSITORY_HASH_ID` varchar(64) null comment '代码库hashId';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND COLUMN_NAME = 'EXTERNAL_NAME') THEN
    ALTER TABLE `T_PIPELINE_WEBHOOK`
        ADD COLUMN `EXTERNAL_NAME` varchar(255) DEFAULT null COMMENT '代码库平台仓库名';
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                    AND COLUMN_NAME = 'ATOM_VERSION') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK MODIFY COLUMN ATOM_VERSION varchar(30)  NULL COMMENT '插件版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PIPELINE_TRIGGER_DETAIL'
                     AND INDEX_NAME = 'IDX_PROJECT_PIPELINE_ID') THEN
        ALTER TABLE T_PIPELINE_TRIGGER_DETAIL ADD INDEX `IDX_PROJECT_PIPELINE_ID` (`PROJECT_ID`, `PIPELINE_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_PIPELINE_CALLBACK'
                        AND COLUMN_NAME = 'FAILURE_TIME') THEN
        ALTER TABLE T_PROJECT_PIPELINE_CALLBACK
            ADD COLUMN `FAILURE_TIME` datetime COMMENT '失败时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_SUMMARY'
                    AND COLUMN_NAME = 'DEBUG_BUILD_NUM') THEN
    ALTER TABLE `T_PIPELINE_BUILD_SUMMARY`
        ADD COLUMN `DEBUG_BUILD_NUM` int(11) DEFAULT '0' COMMENT '调试构建次数';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_SUMMARY'
                    AND COLUMN_NAME = 'DEBUG_BUILD_NO') THEN
    ALTER TABLE `T_PIPELINE_BUILD_SUMMARY`
        ADD COLUMN `DEBUG_BUILD_NO` int(11) DEFAULT NULL COMMENT '调试构建号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'LATEST_VERSION_STATUS') THEN
    ALTER TABLE `T_PIPELINE_INFO`
        ADD COLUMN `LATEST_VERSION_STATUS` varchar(64) DEFAULT NULL COMMENT '最新分布版本状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'VERSION_NAME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `VERSION_NAME` varchar(64) DEFAULT NULL COMMENT '版本名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'YAML') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `YAML` mediumtext COMMENT 'YAML编排';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'VERSION_NUM') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `VERSION_NUM` int(11) DEFAULT NULL COMMENT '流水线发布版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'PIPELINE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `PIPELINE_VERSION` int(11) DEFAULT NULL COMMENT '流水线模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'TRIGGER_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `TRIGGER_VERSION` int(11) DEFAULT NULL COMMENT '触发器模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'SETTING_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `SETTING_VERSION` int(11) DEFAULT NULL COMMENT '关联的流水线设置版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'VERSION') THEN
    ALTER TABLE `T_PIPELINE_SETTING`
        ADD COLUMN `VERSION` int(11) DEFAULT '1' COMMENT '设置版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'SUCCESS_SUBSCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_SETTING`
        ADD COLUMN `SUCCESS_SUBSCRIPTION` text COMMENT '成功订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'FAILURE_SUBSCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_SETTING`
        ADD COLUMN `FAILURE_SUBSCRIPTION` text COMMENT '失败订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'YAML') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `YAML` mediumtext COMMENT 'YAML编排';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'VERSION_NUM') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `VERSION_NUM` int(11) DEFAULT NULL COMMENT '流水线发布版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'PIPELINE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `PIPELINE_VERSION` int(11) DEFAULT NULL COMMENT '流水线模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'TRIGGER_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `TRIGGER_VERSION` int(11) DEFAULT NULL COMMENT '触发器模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'SETTING_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `SETTING_VERSION` int(11) DEFAULT NULL COMMENT '关联的流水线设置版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'BASE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `BASE_VERSION` int(11) DEFAULT NULL COMMENT '草稿的来源版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'DEBUG_BUILD_ID') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `DEBUG_BUILD_ID` varchar(64) DEFAULT NULL COMMENT '调试构建ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'STATUS') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `STATUS` varchar(16) DEFAULT NULL COMMENT '版本状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'BRANCH_ACTION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `BRANCH_ACTION` varchar(32) DEFAULT NULL COMMENT '分支状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'DESCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `DESCRIPTION` text COMMENT '版本变更说明';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'UPDATE_TIME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'NAME') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `NAME` varchar(255) DEFAULT NULL COMMENT '名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'DESC') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `DESC` varchar(1024) DEFAULT NULL COMMENT '描述';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'LABELS') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `LABELS` text DEFAULT NULL COMMENT '版本修改的标签';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'RUN_LOCK_TYPE') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `RUN_LOCK_TYPE` int(11) DEFAULT '1' COMMENT '运行并发配置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'WAIT_QUEUE_TIME_SECOND') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `WAIT_QUEUE_TIME_SECOND` int(11) DEFAULT '7200' COMMENT '最大排队时长';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'MAX_QUEUE_SIZE') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `MAX_QUEUE_SIZE` int(11) DEFAULT '10' COMMENT '最大排队数量';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'BUILD_NUM_RULE') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `BUILD_NUM_RULE` varchar(512) DEFAULT NULL COMMENT '构建号生成规则';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_GROUP') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `CONCURRENCY_GROUP` varchar(255) DEFAULT NULL COMMENT '并发时,设定的group';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_CANCEL_IN_PROGRESS') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `CONCURRENCY_CANCEL_IN_PROGRESS` bit(1) DEFAULT b'0' COMMENT '并发时,是否相同group取消正在执行的流水线';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'PIPELINE_AS_CODE_SETTINGS') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `PIPELINE_AS_CODE_SETTINGS` varchar(512) DEFAULT NULL COMMENT 'YAML流水线相关配置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'SUCCESS_SUBSCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `SUCCESS_SUBSCRIPTION` text COMMENT '成功订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'FAILURE_SUBSCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_SETTING_VERSION`
        ADD COLUMN `FAILURE_SUBSCRIPTION` text COMMENT '失败订阅设置';
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE'
                    AND COLUMN_NAME = 'DESC') THEN
    ALTER TABLE `T_TEMPLATE`
        ADD COLUMN `DESC` varchar(1024) DEFAULT NULL COMMENT '描述';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'VERSION_NAME') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
        ADD COLUMN `VERSION_NAME` varchar(64) DEFAULT NULL COMMENT '正式版本名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TIMER'
                    AND COLUMN_NAME = 'REPO_HASH_ID') THEN
    ALTER TABLE T_PIPELINE_TIMER
        ADD COLUMN `REPO_HASH_ID` varchar(64) COMMENT '代码库HASH ID';
    END IF;

     IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TIMER'
                    AND COLUMN_NAME = 'BRANCHS') THEN
    ALTER TABLE T_PIPELINE_TIMER
        ADD COLUMN `BRANCHS` text  COMMENT '分支列表';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TIMER'
                    AND COLUMN_NAME = 'NO_SCM') THEN
    ALTER TABLE T_PIPELINE_TIMER
        ADD COLUMN `NO_SCM` bit(1)  DEFAULT FALSE COMMENT '源代码未更新则不触发构建';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_PIPELINE_CALLBACK'
                        AND COLUMN_NAME = 'SECRET_PARAM') THEN
        ALTER TABLE T_PROJECT_PIPELINE_CALLBACK
            ADD COLUMN `SECRET_PARAM` text DEFAULT NULL COMMENT '鉴权参数';
    END IF;

	IF NOT EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                AND COLUMN_NAME = 'UPDATER') THEN
    ALTER TABLE T_PIPELINE_RESOURCE_VERSION ADD `UPDATER` varchar(64) DEFAULT NULL COMMENT '最近更新人';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
