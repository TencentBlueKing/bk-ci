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
                        AND TABLE_NAME = 'T_NOTICE'
                        AND COLUMN_NAME = 'SERVICE_NAME') THEN
        ALTER TABLE T_NOTICE
            ADD SERVICE_NAME varchar(1024) NULL COMMENT '服务名称' ;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
