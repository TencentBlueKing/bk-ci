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
                    AND COLUMN_NAME = 'AGENT_TYPE') THEN

        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`
            ADD COLUMN `AGENT_TYPE` varchar(36) DEFAULT 'BUILD' COMMENT '第三方构建机类型';
        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`    
            ADD COLUMN `CREATE_WORKSPACE_NAME` varchar(128) NULL COMMENT '云桌面工作空间名称';
        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`    
            ADD CONSTRAINT CREATE_WORKSPACE_NAME UNIQUE KEY (`CREATE_WORKSPACE_NAME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENV'
                    AND COLUMN_NAME = 'ENV_NODE_TYPE') THEN

        ALTER TABLE `T_ENV`
            ADD COLUMN `ENV_NODE_TYPE` varchar(32) DEFAULT 'NODE' COMMENT '环境节点类型（节点静态环境{NODE}|标签动态环境{TAG}';
    END IF;
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
