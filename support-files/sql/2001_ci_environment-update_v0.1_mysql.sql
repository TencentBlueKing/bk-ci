USE devops_ci_environment;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'BIZ_ID') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `BIZ_ID` bigint(20) DEFAULT NULL COMMENT '所属业务';
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'BIZ_ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
        ALTER TABLE `T_NODE`
            CHANGE `BIZ_ID` `BIZ_ID` bigint(20) DEFAULT NULL;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();