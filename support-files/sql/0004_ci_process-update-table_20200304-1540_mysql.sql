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
                    AND COLUMN_NAME = 'INSTANCE_TYPE') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD COLUMN `INSTANCE_TYPE` VARCHAR(32) NOT NULL DEFAULT 'CONSTRAINT' COMMENT '实例化类型：FREEDOM 自由模式  CONSTRAINT 约束模式' AFTER `PIPELINE_ID`;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'INSTANCE_TYPE'
                        AND COLUMN_TYPE = 'VARCHAR') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            CHANGE `INSTANCE_TYPE` `INSTANCE_TYPE` VARCHAR(32) NOT NULL DEFAULT 'CONSTRAINT' COMMENT '实例化类型：FREEDOM 自由模式  CONSTRAINT 约束模式';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'ROOT_TEMPLATE_ID') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD COLUMN `ROOT_TEMPLATE_ID` VARCHAR(32) NULL COMMENT '源模板ID' AFTER `INSTANCE_TYPE`;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'ROOT_TEMPLATE_ID'
                        AND COLUMN_TYPE = 'VARCHAR') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            CHANGE `ROOT_TEMPLATE_ID` `ROOT_TEMPLATE_ID` VARCHAR(32) NULL COMMENT '源模板ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND INDEX_NAME = 'ROOT_TEMPLATE_ID') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD INDEX ROOT_TEMPLATE_ID (`ROOT_TEMPLATE_ID`);
    END IF;    

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
