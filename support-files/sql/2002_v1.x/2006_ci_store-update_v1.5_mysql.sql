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
                    AND TABLE_NAME = 'T_STORE_SENSITIVE_CONF'
                    AND COLUMN_NAME = 'FIELD_TYPE') THEN
        ALTER TABLE T_STORE_SENSITIVE_CONF ADD COLUMN `FIELD_TYPE` VARCHAR(16) DEFAULT 'BACKEND' COMMENT '字段类型';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();