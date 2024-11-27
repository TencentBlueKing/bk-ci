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

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_MIGRATION'
                    AND COLUMN_NAME = 'ERROR_MESSAGE') THEN
    ALTER TABLE T_AUTH_MIGRATION
        ADD COLUMN `ERROR_MESSAGE` text DEFAULT NULL COMMENT '错误信息';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_IAM_CALLBACK'
                    AND INDEX_NAME = 'UNIQ_RESOURCE') THEN
    ALTER TABLE T_AUTH_IAM_CALLBACK ADD UNIQUE INDEX `UNIQ_RESOURCE` (`RESOURCE`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP'
                    AND INDEX_NAME = 'PROJECT_CODE_RELATION_ID_IDX') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP ADD INDEX `PROJECT_CODE_RELATION_ID_IDX` (`PROJECT_CODE`,`RELATION_ID`);
    END IF;

    IF EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_AUTH_RESOURCE'
                        AND COLUMN_NAME = 'RESOURCE_CODE'
                        AND COLLATION_NAME != 'utf8mb4_bin'
                        ) THEN
    ALTER TABLE T_AUTH_RESOURCE
        MODIFY `RESOURCE_CODE` varchar(255) collate utf8mb4_bin not null comment '资源ID';
    END IF;

     IF EXISTS(SELECT 1
                          FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP'
                            AND COLUMN_NAME = 'RESOURCE_CODE'
                            AND COLLATION_NAME != 'utf8mb4_bin'
                            ) THEN
        ALTER TABLE T_AUTH_RESOURCE_GROUP
            MODIFY `RESOURCE_CODE` varchar(255) collate utf8mb4_bin not null comment '资源ID';
        END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP_CONFIG'
                    AND COLUMN_NAME = 'GROUP_TYPE') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP_CONFIG ADD COLUMN `GROUP_TYPE` Int(2) NOT NULL DEFAULT 0 COMMENT '用户组类型 0-默认组 1-自定义组' AFTER `CREATE_MODE`;
    END IF;
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
