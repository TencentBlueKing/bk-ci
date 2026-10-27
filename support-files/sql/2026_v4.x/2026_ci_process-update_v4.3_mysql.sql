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
                    AND TABLE_NAME = 'T_PIPELINE_TIMER'
                    AND COLUMN_NAME = 'YAML_BRANCH') THEN
        ALTER TABLE T_PIPELINE_TIMER
            ADD COLUMN `YAML_BRANCH` varchar(255) DEFAULT NULL COMMENT 'PAC 非默认分支定时：到点按该分支 YAML 执行';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                    AND INDEX_NAME = 'idx_proj_pipeline') THEN
        ALTER TABLE `T_PIPELINE_BUILD_STAGE`
            ADD INDEX `idx_proj_pipeline` (`PROJECT_ID`, `PIPELINE_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TRIGGER_DETAIL'
                    AND INDEX_NAME = 'IDX_PROJECT_PIPELINE_CREATE_TIME') THEN
        ALTER TABLE T_PIPELINE_TRIGGER_DETAIL
            ADD INDEX `IDX_PROJECT_PIPELINE_CREATE_TIME` (`PROJECT_ID`, `PIPELINE_ID`, `CREATE_TIME`);
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_TRIGGER_DETAIL'
                AND INDEX_NAME = 'IDX_PROJECT_PIPELINE_ID') THEN
        ALTER TABLE T_PIPELINE_TRIGGER_DETAIL DROP INDEX `IDX_PROJECT_PIPELINE_ID`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
