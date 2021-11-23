USE devops_ci_ticket;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_ticket_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_ticket_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
    
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_CERT'
                    AND COLUMN_NAME = 'CERT_ID') THEN
        ALTER TABLE T_CERT
            ADD COLUMN `CERT_ID` VARCHAR(128) NOT NULL COMMENT '证书ID' AFTER PROJECT_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_CERT'
                        AND COLUMN_NAME = 'CERT_ID'
                        AND COLUMN_TYPE = 'varchar(128)') THEN
        ALTER TABLE T_CERT
            CHANGE `CERT_ID` `CERT_ID` VARCHAR(128) NOT NULL  AFTER PROJECT_ID;
    END IF;
    
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_ticket_schema_update();