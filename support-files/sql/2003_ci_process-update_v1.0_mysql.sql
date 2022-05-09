USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_REPORT'
                AND INDEX_NAME = 'PROJECT_ID_PIPELINE_ID_BUILD_ID_ELEMENT_ID') THEN
        ALTER TABLE T_REPORT DROP INDEX `PROJECT_ID_PIPELINE_ID_BUILD_ID_ELEMENT_ID`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'PROJECT_PIPELINE_BUILD_IDX') THEN
        ALTER TABLE T_REPORT ADD INDEX `PROJECT_PIPELINE_BUILD_IDX` (`PROJECT_ID`,`PIPELINE_ID`,`BUILD_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'MAX_PIPELINE_RES_NUM') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `MAX_PIPELINE_RES_NUM` int(11) DEFAULT '50' COLLATE utf8mb4_bin COMMENT '保存流水线编排的最大个数';
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
