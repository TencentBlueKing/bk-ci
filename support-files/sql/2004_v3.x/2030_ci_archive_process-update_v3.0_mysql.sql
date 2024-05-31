USE devops_ci_archive_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_archive_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_archive_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'INSTANCE_ERROR_INFO') THEN
    ALTER TABLE T_TEMPLATE_PIPELINE
        ADD COLUMN `INSTANCE_ERROR_INFO` text null comment '实例化错误信息';
    END IF;
    
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_RECORD_TASK'
                    AND COLUMN_NAME = 'ASYNC_STATUS') THEN
    ALTER TABLE `T_PIPELINE_BUILD_RECORD_TASK`
        ADD COLUMN `ASYNC_STATUS` varchar(32) DEFAULT NULL COMMENT '插件异步执行状态';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_archive_process_schema_update();
