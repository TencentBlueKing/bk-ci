USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_STATISTICS_TOTAL'
                    AND COLUMN_NAME = 'PIPELINE_NUM') THEN
        ALTER TABLE T_STORE_STATISTICS_TOTAL ADD COLUMN `PIPELINE_NUM` INT(11) DEFAULT '0' COMMENT '流水线数量';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_STATISTICS_TOTAL'
                    AND COLUMN_NAME = 'RECENT_EXECUTE_NUM') THEN
        ALTER TABLE T_STORE_STATISTICS_TOTAL ADD COLUMN `RECENT_EXECUTE_NUM` INT(11) DEFAULT '0' COMMENT '最近执行次数';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();