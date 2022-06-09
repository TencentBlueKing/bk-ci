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
                    AND TABLE_NAME = 'T_ENVIRONMENT_THIRDPARTY_AGENT'
                    AND COLUMN_NAME = 'AGENT_PROPS') THEN

        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`
            ADD COLUMN `AGENT_PROPS` text COMMENT 'agent config 配置项Json';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
