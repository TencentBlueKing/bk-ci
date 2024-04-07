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

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
