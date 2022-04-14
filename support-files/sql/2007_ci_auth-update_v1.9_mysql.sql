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
                            AND TABLE_NAME = 'T_AUTH_GROUP_INFO'
                            AND COLUMN_NAME = 'DESC') THEN
    ALTER TABLE T_GROUP_INFO ADD COLUMN `DESC` VARCHAR (255) COMMENT '用户组描述';
    END IF;
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
