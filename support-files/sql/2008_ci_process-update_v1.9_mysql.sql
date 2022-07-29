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


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
