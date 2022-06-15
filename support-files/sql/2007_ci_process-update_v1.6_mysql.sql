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
                        AND TABLE_NAME = 'T_TEMPLATE_INSTANCE_BASE'
                        AND COLUMN_NAME = 'TEMPLATE_ID') THEN
        ALTER TABLE T_TEMPLATE_INSTANCE_BASE ADD COLUMN `TEMPLATE_ID` VARCHAR(32) DEFAULT '' COMMENT '模板ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'DELETED') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE ADD COLUMN `DELETED` bit(1) DEFAULT b'0' COMMENT '流水线已被软删除';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_PIPELINE_CALLBACK'
                        AND COLUMN_NAME = 'ENABLE') THEN
        ALTER TABLE T_PROJECT_PIPELINE_CALLBACK ADD COLUMN `ENABLE` bit(1) NOT NULL DEFAULT b'1' COMMENT '启用';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_PIPELINE_INFO'
                  AND COLUMN_NAME = 'ID') THEN
        ALTER TABLE T_PIPELINE_INFO ADD COLUMN `ID` BIGINT NULL AUTO_INCREMENT,ADD KEY(`ID`) COMMENT '主键ID';

    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                        AND COLUMN_NAME = 'READ_ONLY') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR ADD COLUMN READ_ONLY BIT(1) NULL COMMENT '是否只读';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                        AND COLUMN_NAME = 'CHECK_IN') THEN
        ALTER TABLE T_PIPELINE_BUILD_STAGE ADD COLUMN `CHECK_IN` mediumtext NULL COMMENT '准入检查配置';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                        AND COLUMN_NAME = 'CHECK_OUT') THEN
        ALTER TABLE T_PIPELINE_BUILD_STAGE ADD COLUMN `CHECK_OUT` mediumtext NULL COMMENT '准出检查配置';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'PIPELINE_NAME_PINYIN') THEN
        ALTER TABLE T_PIPELINE_INFO
            ADD COLUMN `PIPELINE_NAME_PINYIN` varchar(1300) DEFAULT NULL COMMENT '流水线名称拼音';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
