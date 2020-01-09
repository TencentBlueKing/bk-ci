USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

	ALTER TABLE T_IMAGE_AGENT_TYPE CHANGE COLUMN `IMAGE_CODE` `IMAGE_CODE` varchar(64) COMMENT '镜像代码';
	
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DOCKER_FILE_TYPE') THEN
        ALTER TABLE T_IMAGE ADD COLUMN `DOCKER_FILE_TYPE` varchar(32) NOT NULL DEFAULT 'INPUT' COMMENT 'dockerFile类型（INPUT：手动输入，*_LINK：链接）';
    END IF;
	
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DOCKER_FILE_CONTENT') THEN
        ALTER TABLE T_IMAGE ADD COLUMN `DOCKER_FILE_CONTENT` text NOT NULL COMMENT 'dockerFile内容';
    END IF;
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();