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
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'OAUTH_OPERATOR') THEN
        ALTER TABLE T_GIT_BASIC_SETTING ADD COLUMN `OAUTH_OPERATOR` varchar(32) NOT NULL DEFAULT '' COMMENT 'OAUTH身份的修改者';
    END IF;
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_stream_schema_update();
