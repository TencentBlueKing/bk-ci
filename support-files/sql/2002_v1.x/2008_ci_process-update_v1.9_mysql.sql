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
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'PLATFORM_CODE') THEN
        alter table T_PIPELINE_BUILD_TASK add column `PLATFORM_CODE` varchar(64) DEFAULT NULL COMMENT '对接平台代码';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'PLATFORM_ERROR_CODE') THEN
        alter table T_PIPELINE_BUILD_TASK add column `PLATFORM_ERROR_CODE` int(11) DEFAULT NULL COMMENT '对接平台错误码';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'CLEAN_VARIABLES_WHEN_RETRY') THEN
        ALTER TABLE T_PIPELINE_SETTING ADD COLUMN `CLEAN_VARIABLES_WHEN_RETRY` BIT(1) DEFAULT b'0' COMMENT '重试时清理变量表';
    END IF;
	
	IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                AND INDEX_NAME = 'STAT_PROJECT_RUN') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK DROP INDEX `STAT_PROJECT_RUN`;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                    AND INDEX_NAME = 'INX_TPMT_PROJECT_ATOM') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK ADD INDEX `INX_TPMT_PROJECT_ATOM` (`PROJECT_ID`,`ATOM_CODE`);
    END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_PIPELINE_BUILD_COMMITS'
                  AND COLUMN_NAME = 'CHANNEL') THEN
      ALTER TABLE T_PIPELINE_BUILD_COMMITS ADD COLUMN `CHANNEL` varchar(32) DEFAULT NULL;
  END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'PIPELINE_AS_CODE_SETTINGS') THEN
        alter table T_PIPELINE_SETTING add column `PIPELINE_AS_CODE_SETTINGS` varchar(512) DEFAULT NULL COMMENT 'YAML流水线相关配置';
    END IF;

  IF NOT EXISTS(SELECT 1  
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_COMMITS'
                        AND COLUMN_NAME = 'ACTION') THEN
        alter table T_PIPELINE_BUILD_COMMITS add column `ACTION` varchar(64) DEFAULT NULL;
    END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPORT'
                        AND COLUMN_NAME = 'TASK_NAME') THEN
        alter table T_REPORT add column `TASK_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '任务名称';
    END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPORT'
                        AND COLUMN_NAME = 'ATOM_CODE') THEN
        alter table T_REPORT add column `ATOM_CODE` varchar(128) NOT NULL DEFAULT '' COMMENT '插件的唯一标识';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND INDEX_NAME = 'IDX_PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_VIEW ADD INDEX `IDX_PROJECT_ID` (`PROJECT_ID`);
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND INDEX_NAME = 'PROJECT_NAME') THEN
        ALTER TABLE T_PIPELINE_VIEW DROP INDEX `PROJECT_NAME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_VIEW'
                        AND COLUMN_NAME = 'VIEW_TYPE') THEN
        ALTER TABLE T_PIPELINE_VIEW ADD COLUMN `VIEW_TYPE` int NOT NULL DEFAULT '1' COMMENT '1:动态流水线组 , 2:静态流水线组';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                        AND COLUMN_NAME = 'REFER_FLAG') THEN
       ALTER TABLE T_PIPELINE_RESOURCE_VERSION ADD COLUMN `REFER_FLAG` bit(1) DEFAULT NULL COMMENT '是否还有构建记录引用该版本标识';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                        AND COLUMN_NAME = 'REFER_COUNT') THEN
        ALTER TABLE T_PIPELINE_RESOURCE_VERSION ADD COLUMN `REFER_COUNT` int(20) DEFAULT NULL COMMENT '关联构建记录总数';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                        AND COLUMN_NAME = 'EXECUTE_COUNT') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY ADD COLUMN `EXECUTE_COUNT` int(11) DEFAULT NULL COMMENT '执行次数';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
