USE devops_ci_dispatch;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE'
                        AND COLUMN_NAME = 'DOCKER_RESOURCE_OPTION') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE ADD COLUMN `DOCKER_RESOURCE_OPTION` int(11) DEFAULT 0 COMMENT 'Docker资源配置';
    END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_RUNNING_JOBS'
                        AND COLUMN_NAME = 'EXECUTE_COUNT') THEN
        ALTER TABLE T_DISPATCH_RUNNING_JOBS ADD COLUMN `EXECUTE_COUNT` int(11) DEFAULT 0 COMMENT '重试次数';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
