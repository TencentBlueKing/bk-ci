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
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'STAGE_STATUS') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `STAGE_STATUS` text COLLATE utf8mb4_bin DEFAULT NULL COMMENT '流水线各阶段状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                    AND COLUMN_NAME = 'CONDITIONS') THEN
        ALTER TABLE T_PIPELINE_BUILD_STAGE
            ADD COLUMN `CONDITIONS` mediumtext COLLATE utf8mb4_bin COMMENT '状况';
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
