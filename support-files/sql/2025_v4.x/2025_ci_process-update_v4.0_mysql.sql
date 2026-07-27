USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- 定时触发 IANA 时区；存量默认 Asia/Shanghai（东八区）
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TIMER'
                    AND COLUMN_NAME = 'TIME_ZONE') THEN
        ALTER TABLE `T_PIPELINE_TIMER`
            ADD COLUMN `TIME_ZONE` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai'
                COMMENT '定时触发IANA时区，存量默认Asia/Shanghai' AFTER `START_PARAM`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
