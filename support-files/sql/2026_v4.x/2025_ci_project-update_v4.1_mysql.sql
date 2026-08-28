USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'project_scope') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `project_scope` int(10) NOT NULL DEFAULT '0'
                COMMENT '项目组织形态：0-团队项目，1-个人项目';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'PROJECT_SCOPE') THEN
    ALTER TABLE T_PROJECT_APPROVAL
        ADD COLUMN `PROJECT_SCOPE` int(10) NOT NULL DEFAULT '0'
                    COMMENT '项目组织形态：0-团队项目，1-个人项目';
    END IF;

    -- 存量 tenant_id='default' 刷成 NULL，列默认改为可空（单租户不过滤）
    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PROJECT'
                AND COLUMN_NAME = 'tenant_id') THEN
        IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'tenant_id'
                    AND COLUMN_DEFAULT = 'default') THEN
            ALTER TABLE T_PROJECT
                MODIFY COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID，存量单租户可为空';
        END IF;
        UPDATE T_PROJECT SET `tenant_id` = NULL WHERE `tenant_id` = 'default';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                AND COLUMN_NAME = 'TENANT_ID') THEN
        IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'TENANT_ID'
                    AND COLUMN_DEFAULT = 'default') THEN
            ALTER TABLE T_PROJECT_APPROVAL
                MODIFY COLUMN `TENANT_ID` varchar(32) DEFAULT NULL COMMENT '租户ID，存量单租户可为空';
        END IF;
        UPDATE T_PROJECT_APPROVAL SET `TENANT_ID` = NULL WHERE `TENANT_ID` = 'default';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;
