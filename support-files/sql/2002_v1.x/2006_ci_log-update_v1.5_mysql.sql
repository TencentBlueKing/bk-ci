USE devops_ci_log;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_log_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_log_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
    
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_STATUS'
                    AND COLUMN_NAME = 'MODE') THEN
        ALTER TABLE T_LOG_STATUS ADD COLUMN `MODE` VARCHAR(32) NULL DEFAULT NULL COMMENT '模式';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_log_schema_update();