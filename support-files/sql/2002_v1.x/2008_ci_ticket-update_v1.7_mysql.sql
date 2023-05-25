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
                    AND TABLE_NAME = 'T_CREDENTIAL'
                    AND COLUMN_NAME = 'ALLOW_ACROSS_PROJECT') THEN
        ALTER TABLE `T_CREDENTIAL` 
            ADD COLUMN `ALLOW_ACROSS_PROJECT` bit(1) NOT NULL DEFAULT 0;
    END IF;
    
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_ticket_schema_update();