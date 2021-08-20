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
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'PAUSE_REVIEWERS') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `PAUSE_REVIEWERS` text null comment '暂停插件可操作继续的用户';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                    AND COLUMN_NAME = 'PAUSE_REVIEWERS') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK
            ADD COLUMN `PAUSE_REVIEWERS` text null comment '暂停插件可操作继续的用户';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
