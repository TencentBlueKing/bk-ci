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
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUDIT_RESOURCE'
                    AND INDEX_NAME = 'IDX_SEARCH_ID') THEN
        alter table T_AUDIT_RESOURCE ADD INDEX
            IDX_SEARCH_ID (`RESOURCE_TYPE`, `PROJECT_ID`, `RESOURCE_ID`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
