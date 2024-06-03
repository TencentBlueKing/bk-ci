USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'BUILD_NUM_RULE') THEN
        ALTER TABLE T_PIPELINE_SETTING ADD COLUMN `BUILD_NUM_RULE` VARCHAR(512) COMMENT '构建号生成规则';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                        AND COLUMN_NAME = 'BUILD_NUM_ALIAS') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY ADD COLUMN `BUILD_NUM_ALIAS` VARCHAR(256) COMMENT '自定义构建号';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_SUMMARY'
                        AND COLUMN_NAME = 'BUILD_NUM_ALIAS') THEN
        ALTER TABLE T_PIPELINE_BUILD_SUMMARY ADD COLUMN `BUILD_NUM_ALIAS` VARCHAR(256) COMMENT '自定义构建版本号';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
