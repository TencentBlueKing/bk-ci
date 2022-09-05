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

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENV'
                    AND COLUMN_NAME = 'ENV_HASH_ID') THEN

        ALTER TABLE `T_ENV`
            ADD COLUMN `ENV_HASH_ID` varchar(64) DEFAULT NULL COMMENT '环境哈希ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'NODE_HASH_ID') THEN

        ALTER TABLE `T_NODE`
            ADD COLUMN `NODE_HASH_ID` varchar(64) DEFAULT NULL COMMENT '节点哈希ID';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
