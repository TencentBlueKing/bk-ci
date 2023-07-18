USE devops_ci_auth;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_auth_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_auth_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_MIGRATION'
                    AND COLUMN_NAME = 'ROUTER_TAG') THEN
    ALTER TABLE T_AUTH_MIGRATION
        ADD COLUMN `ROUTER_TAG` varchar(32) DEFAULT NULL COMMENT '迁移项目的网关路由tags';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_RESOURCE'
                    AND INDEX_NAME = 'RESOURCE_TYPE_UPDATE_TIME_IDX') THEN
    ALTER TABLE T_AUTH_RESOURCE ADD INDEX `RESOURCE_TYPE_UPDATE_TIME_IDX` (`RESOURCE_TYPE`,`UPDATE_TIME`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
