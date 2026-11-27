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
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'CANCEL_SUBSCRIPTION') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `CANCEL_SUBSCRIPTION` text COMMENT '取消订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'PUBLISH_SUBSCRIPTION') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `PUBLISH_SUBSCRIPTION` text COMMENT '发布新版本订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'CANCEL_SUBSCRIPTION') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `CANCEL_SUBSCRIPTION` text COMMENT '取消订阅设置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'PUBLISH_SUBSCRIPTION') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `PUBLISH_SUBSCRIPTION` text COMMENT '发布新版本订阅设置';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_archive_process_schema_update();
