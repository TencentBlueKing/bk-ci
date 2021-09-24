USE devops_ci_stream;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_stream_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_stream_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_REQUEST_EVENT_NOT_BUILD'
                    AND COLUMN_NAME = 'BRANCH') THEN
        ALTER TABLE T_GIT_REQUEST_EVENT_NOT_BUILD ADD COLUMN `BRANCH` varchar(1024) NULL COMMENT 'git分支' AFTER `VERSION`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_stream_schema_update();
