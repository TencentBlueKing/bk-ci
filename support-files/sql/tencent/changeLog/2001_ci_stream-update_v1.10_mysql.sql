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
                    AND TABLE_NAME = 'T_GIT_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'DIRECTORY') THEN
        ALTER TABLE T_GIT_PIPELINE_RESOURCE ADD COLUMN `DIRECTORY` varchar(512) NOT NULL DEFAULT '.ci/' COMMENT '文件子路径';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_GIT_PIPELINE_RESOURCE'
                  AND INDEX_NAME = 'IDX_DIRECTORY') THEN
		ALTER TABLE T_GIT_PIPELINE_RESOURCE ADD INDEX IDX_DIRECTORY (`DIRECTORY`);
	END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_stream_schema_update();
