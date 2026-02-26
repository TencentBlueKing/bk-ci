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
                    AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP'
                    AND COLUMN_NAME = 'APPLY_DISABLE') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP ADD `APPLY_DISABLE` bit(1) DEFAULT NULL COMMENT '是否禁止申请，当true为禁止';
    END IF;

     IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP_MEMBER'
                    AND COLUMN_NAME = 'JOINED_AT') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP_MEMBER ADD `JOINED_AT` datetime DEFAULT NULL COMMENT '加入时间';
    END IF;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
