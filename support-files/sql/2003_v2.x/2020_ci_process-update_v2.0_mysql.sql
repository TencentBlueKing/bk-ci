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

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
