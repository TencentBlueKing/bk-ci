USE devops_ci_notify;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_notify_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_notify_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
    
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NOTIFY_EMAIL'
                    AND COLUMN_NAME = 'BODY') THEN
        ALTER TABLE T_NOTIFY_EMAIL
            ADD COLUMN `BODY` mediumtext NOT NULL;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NOTIFY_EMAIL'
                        AND COLUMN_NAME = 'BODY'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_NOTIFY_EMAIL
            CHANGE `BODY` `BODY` mediumtext NOT NULL;
    END IF;
    
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_notify_schema_update();