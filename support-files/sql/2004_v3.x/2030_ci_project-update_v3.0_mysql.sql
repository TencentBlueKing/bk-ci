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
                        AND TABLE_NAME = 'T_SERVICE'
                        AND COLUMN_NAME = 'DOC_URL') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `DOC_URL` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文档链接';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                        AND COLUMN_NAME = 'PROPERTIES') THEN
        ALTER TABLE T_PROJECT_APPROVAL
            ADD COLUMN `PROPERTIES` text null DEFAULT NULL comment '项目其他配置';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
