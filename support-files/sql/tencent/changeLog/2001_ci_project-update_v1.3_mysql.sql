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
                    AND COLUMN_NAME = 'router_tag') THEN
        ALTER TABLE T_PROJECT ADD COLUMN `router_tag` VARCHAR(32) NULL COMMENT 'consul路由tags' AFTER `pipeline_limit`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'other_router_tags') THEN
        ALTER TABLE T_PROJECT ADD COLUMN `other_router_tags` VARCHAR(32) NULL COMMENT '扩展系统consul路由tags';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
