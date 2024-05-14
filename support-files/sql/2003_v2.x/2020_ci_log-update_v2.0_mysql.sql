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
                    AND COLUMN_NAME = 'USER_JOB_ID') THEN
        ALTER TABLE T_LOG_STATUS ADD COLUMN `USER_JOB_ID` varchar(128) NULL COMMENT '真正的jobId，已经存在的 JOB_ID 字段其实是 container hash id';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_STATUS'
                    AND COLUMN_NAME = 'STEP_ID') THEN
        ALTER TABLE T_LOG_STATUS ADD COLUMN `STEP_ID` varchar(64) NULL COMMENT '用户填写的插件id';
    END IF;
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_log_schema_update();