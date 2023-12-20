USE devops_ci_dispatch;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                        AND COLUMN_NAME = 'ENV_ID') THEN
        ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD` 
        ADD COLUMN `ENV_ID` bigint(20) NULL COMMENT '第三方构建所属环境';

    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                        AND COLUMN_NAME = 'JOB_ID') THEN
        ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD` 
        ADD COLUMN `JOB_ID` VARCHAR(32) NULL COMMENT '当前构建所属jobid';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
