USE devops_ci_plugin;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_plugin_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_plugin_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PLUGIN_GIT_CHECK'
                    AND COLUMN_NAME = 'CONTEXT') THEN
        ALTER TABLE `T_PLUGIN_GIT_CHECK` 
			ADD COLUMN `CONTEXT` VARCHAR(255) COMMENT '内容';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_plugin_schema_update();