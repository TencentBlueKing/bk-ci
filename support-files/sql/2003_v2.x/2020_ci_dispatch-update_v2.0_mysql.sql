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
                    AND TABLE_NAME = 'T_DISPATCH_RUNNING_JOBS'
                    AND COLUMN_NAME = 'CHANNEL_CODE') THEN
    ALTER TABLE T_DISPATCH_RUNNING_JOBS
        ADD COLUMN `CHANNEL_CODE` varchar(128) NOT NULL DEFAULT 'BS' COMMENT '构建来源，包含：BS,CODECC,AM,GIT等';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_QUOTA_PROJECT'
                    AND COLUMN_NAME = 'CHANNEL_CODE') THEN
        ALTER TABLE T_DISPATCH_QUOTA_PROJECT
            ADD COLUMN `CHANNEL_CODE` varchar(128) NOT NULL DEFAULT 'BS' COMMENT '构建来源，包含：BS,CODECC,AM,GIT等';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_RUNNING_JOBS'
                    AND INDEX_NAME = 'IDX_PROJECT_TYPE_CHANNEL') THEN
    ALTER TABLE T_DISPATCH_RUNNING_JOBS ADD INDEX `IDX_PROJECT_TYPE_CHANNEL` (`PROJECT_ID`,`VM_TYPE`,`CHANNEL_CODE`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
