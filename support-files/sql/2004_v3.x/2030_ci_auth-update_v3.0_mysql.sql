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
                    AND COLUMN_NAME = 'DESCRIPTION') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP
        ADD COLUMN `DESCRIPTION` varchar(512) DEFAULT NULL COMMENT '用户组描述';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_AUTH_RESOURCE_GROUP'
                        AND COLUMN_NAME = 'IAM_TEMPLATE_ID') THEN
    ALTER TABLE T_AUTH_RESOURCE_GROUP
        ADD COLUMN `IAM_TEMPLATE_ID` int(20) DEFAULT NULL COMMENT '人员模板ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AUTH_OAUTH2_ACCESS_TOKEN'
                    AND COLUMN_NAME = 'PASS_WORD') THEN
    ALTER TABLE T_AUTH_OAUTH2_ACCESS_TOKEN ADD COLUMN `PASS_WORD` VARCHAR(64) DEFAULT NULL COMMENT '用于密码模式' AFTER `USER_NAME`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
