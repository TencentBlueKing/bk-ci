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
                        AND COLUMN_NAME = 'AGENT_IP') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD COLUMN `AGENT_IP` varchar(128) DEFAULT '' COMMENT '第三方构建机IP';
    END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                        AND COLUMN_NAME = 'NODE_ID') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD COLUMN `NODE_ID` bigint(20) DEFAULT 0 COMMENT '第三方构建机NODE_ID';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
