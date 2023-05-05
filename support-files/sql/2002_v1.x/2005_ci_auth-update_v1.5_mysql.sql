USE devops_ci_auth;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_auth_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_auth_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
	
	IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_AUTH_IAM_CALLBACK'
                AND COLUMN_NAME = 'GATEWAY') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_AUTH_IAM_CALLBACK'
                        AND COLUMN_NAME = 'GATEWAY'
                        AND COLUMN_TYPE = 'varchar(255)') THEN
            ALTER TABLE T_AUTH_IAM_CALLBACK MODIFY COLUMN GATEWAY VARCHAR(255) NOT NULL COMMENT '网关地址';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_AUTH_IAM_CALLBACK'
                AND COLUMN_NAME = 'PATH') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_AUTH_IAM_CALLBACK'
                        AND COLUMN_NAME = 'PATH'
                        AND COLUMN_TYPE = 'varchar(1024)') THEN
            ALTER TABLE T_AUTH_IAM_CALLBACK MODIFY COLUMN PATH VARCHAR(1024) NOT NULL COMMENT '路径';
        END IF;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_auth_schema_update();
