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
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_SUMMARY'
                    AND COLUMN_NAME = 'DEBUG_BUILD_NUM') THEN
    ALTER TABLE `T_PIPELINE_BUILD_SUMMARY`
        ADD COLUMN `DEBUG_BUILD_NUM` int(11) DEFAULT '0' COMMENT '调试构建次数';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_SUMMARY'
                    AND COLUMN_NAME = 'DEBUG_BUILD_NO') THEN
    ALTER TABLE `T_PIPELINE_BUILD_SUMMARY`
        ADD COLUMN `DEBUG_BUILD_NO` int(11) DEFAULT '0' COMMENT '调试构建号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'LATEST_VERSION_STATUS') THEN
    ALTER TABLE `T_PIPELINE_INFO`
        ADD COLUMN `LATEST_VERSION_STATUS` varchar(64) DEFAULT NULL COMMENT '最新分布版本状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'VERSION_NAME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `VERSION_NAME` varchar(64) DEFAULT NULL COMMENT '版本名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'YAML') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `YAML` mediumtext COMMENT 'YAML编排';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'VERSION_NUM') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `VERSION_NUM` int(11) DEFAULT NULL COMMENT '流水线发布版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'PIPELINE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `PIPELINE_VERSION` int(11) DEFAULT '0' COMMENT '流水线模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'TRIGGER_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `TRIGGER_VERSION` int(11) DEFAULT '0' COMMENT '触发器模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'SETTING_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE`
        ADD COLUMN `SETTING_VERSION` int(11) DEFAULT '0' COMMENT '关联的流水线设置版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'YAML') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `YAML` mediumtext COMMENT 'YAML编排';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'VERSION_NUM') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `VERSION_NUM` int(11) DEFAULT NULL COMMENT '流水线发布版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'PIPELINE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `PIPELINE_VERSION` int(11) DEFAULT '0' COMMENT '流水线模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'TRIGGER_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `TRIGGER_VERSION` int(11) DEFAULT '0' COMMENT '触发器模型版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'SETTING_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `SETTING_VERSION` int(11) DEFAULT '0' COMMENT '关联的流水线设置版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'BASE_VERSION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `BASE_VERSION` int(11) DEFAULT NULL COMMENT '草稿的来源版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'DEBUG_BUILD_ID') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `DEBUG_BUILD_ID` varchar(64) DEFAULT NULL COMMENT '调试构建ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'STATUS') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `STATUS` varchar(16) DEFAULT NULL COMMENT '版本状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'BRANCH_ACTION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `BRANCH_ACTION` varchar(32) DEFAULT NULL COMMENT '分支状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'DESCRIPTION') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `DESCRIPTION` text COMMENT '版本变更说明';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'UPDATE_TIME') THEN
    ALTER TABLE `T_PIPELINE_RESOURCE_VERSION`
        ADD COLUMN `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间';
    END IF;

     IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'VERSION_NAME') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
        ADD COLUMN  `VERSION_NAME` varchar(64) DEFAULT NULL COMMENT '正式版本名称';
    END IF;
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'YAML_VERSION') THEN
    ALTER TABLE `T_PIPELINE_BUILD_HISTORY`
        ADD COLUMN `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML的版本标记';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_archive_process_schema_update();
