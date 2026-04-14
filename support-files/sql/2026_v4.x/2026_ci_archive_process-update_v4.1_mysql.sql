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
                    AND COLUMN_NAME = 'ENV_HASH_ID') THEN
    ALTER TABLE T_PIPELINE_SETTING
        ADD COLUMN `ENV_HASH_ID` varchar(256) COMMENT '环境HashId';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'ENV_HASH_ID') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `ENV_HASH_ID` varchar(256) COMMENT '环境HashId';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'NODE_HASH_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
           ADD COLUMN `NODE_HASH_ID` varchar(256) COMMENT '运行节点HashId';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                    AND COLUMN_NAME = 'NODE_HASH_ID') THEN
       ALTER TABLE T_PIPELINE_BUILD_HISTORY_DEBUG
          ADD COLUMN `NODE_HASH_ID` varchar(256) COMMENT '运行节点HashId';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'ENV_NAME') THEN
    ALTER TABLE T_PIPELINE_SETTING
        ADD COLUMN `ENV_NAME` varchar(256) COMMENT '环境名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'ENV_NAME') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `ENV_NAME` varchar(256) COMMENT '环境名称';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                AND COLUMN_NAME = 'TRIGGER_EVENT_TYPE') THEN
       ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
          ADD COLUMN `TRIGGER_EVENT_TYPE` VARCHAR(64) DEFAULT NULL comment '触发事件标识';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                    AND COLUMN_NAME = 'TRIGGER_EVENT_TYPE') THEN
       ALTER TABLE `T_PIPELINE_BUILD_HISTORY_DEBUG`
          ADD COLUMN `TRIGGER_EVENT_TYPE` VARCHAR(64) DEFAULT NULL comment '触发事件标识';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_archive_process_schema_update();
