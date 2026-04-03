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
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_INFO'
                    AND COLUMN_NAME = 'CHANNEL') THEN
       ALTER TABLE `T_PIPELINE_TEMPLATE_INFO`
          ADD COLUMN `CHANNEL` varchar(32) DEFAULT 'BS' COMMENT '来源渠道';
    END IF;

    -- AI自动摘要字段
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'AUTO_SUMMARY') THEN
        ALTER TABLE `T_PIPELINE_INFO`
            ADD COLUMN `AUTO_SUMMARY` text DEFAULT NULL COMMENT 'AI自动生成的流水线摘要' AFTER `LOCKED`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
