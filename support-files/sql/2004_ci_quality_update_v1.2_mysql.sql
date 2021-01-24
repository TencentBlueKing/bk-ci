USE devops_ci_quality;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_quality_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_quality_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_METADATA'
                    AND INDEX_NAME = 'IDX_DATA_TYPE') THEN
        ALTER TABLE T_QUALITY_METADATA
            ADD INDEX `IDX_DATA_TYPE` (`DATA_ID`,`ELEMENT_TYPE`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_RULE'
                    AND INDEX_NAME = 'IDX_PROJECT') THEN
        ALTER TABLE T_QUALITY_RULE
            ADD INDEX `IDX_PROJECT` (`PROJECT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_HISTORY'
                    AND INDEX_NAME = 'IDX_RULE_PROJECT_NUM') THEN
        ALTER TABLE T_HISTORY
            ADD INDEX `IDX_RULE_PROJECT_NUM` (`RULE_ID`,`PROJECT_ID`,`PROJECT_NUM`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_quality_schema_update();
