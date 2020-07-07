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
                    AND TABLE_NAME = 'T_LOG_INDICES_V2'
                    AND COLUMN_NAME = 'LOG_CLUSTER_NAME') THEN
        ALTER TABLE T_LOG_INDICES_V2
            ADD COLUMN `LOG_CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' AFTER ENABLE;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_INDICES_V2'
                    AND COLUMN_NAME = 'USE_CLUSTER') THEN
        ALTER TABLE `T_LOG_INDICES_V2`
        	ADD COLUMN `USE_CLUSTER` bit(1) NOT NULL DEFAULT b'0';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_log_schema_update();
