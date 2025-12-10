USE devops_ci_dispatch;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND COLUMN_NAME = 'TIME_INTERVAL') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD` ADD COLUMN `TIME_INTERVAL` bigint(20) NULL COMMENT '时间间隔';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
