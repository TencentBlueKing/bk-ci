USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
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
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                     AND INDEX_NAME = 'INX_PIPELINE_UPDATE_TIME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD INDEX `INX_PIPELINE_UPDATE_TIME`(`PROJECT_ID`,`PIPELINE_ID`,`UPDATE_TIME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'LOCKED') THEN
    ALTER TABLE T_PIPELINE_INFO
        ADD COLUMN `LOCKED` bit(1) DEFAULT b'0' COMMENT '是否锁定，PAC v3.0新增锁定，取代原来setting表中的LOCK';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
