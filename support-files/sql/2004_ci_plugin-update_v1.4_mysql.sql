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
                    AND TABLE_NAME = 'T_PLUGIN_GITHUB_CHECK'
                    AND COLUMN_NAME = 'CHECK_RUN_NAME') THEN
        ALTER TABLE `T_PLUGIN_GITHUB_CHECK` 
			ADD `CHECK_RUN_NAME` VARCHAR(64) NULL DEFAULT NULL;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PLUGIN_GITHUB_CHECK'
                    AND COLUMN_NAME = 'CHECK_RUN_ID'
					AND COLUMN_TYPE = 'bigint(20)') THEN
        ALTER TABLE `T_PLUGIN_GITHUB_CHECK` 
			MODIFY `CHECK_RUN_ID` BIGINT(20) NOT NULL;
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_plugin_schema_update();