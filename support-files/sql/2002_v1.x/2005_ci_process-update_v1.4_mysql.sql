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
                        AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                        AND COLUMN_NAME = 'ATOM_VERSION') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK ADD COLUMN `ATOM_VERSION` VARCHAR(20) COMMENT '插件版本号';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                        AND COLUMN_NAME = 'CREATE_TIME') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK ADD COLUMN `CREATE_TIME` DATETIME(3) COMMENT '创建时间';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
                        AND COLUMN_NAME = 'UPDATE_TIME') THEN
        ALTER TABLE T_PIPELINE_MODEL_TASK ADD COLUMN `UPDATE_TIME` DATETIME(3) COMMENT '更新时间';
    END IF;
	
	IF NOT EXISTS(SELECT 1
			  FROM information_schema.statistics
			  WHERE TABLE_SCHEMA = db
				 AND TABLE_NAME = 'T_PIPELINE_MODEL_TASK'
				 AND INDEX_NAME = 'INX_TPMT_UPDATE_TIME') THEN
		ALTER TABLE T_PIPELINE_MODEL_TASK ADD INDEX INX_TPMT_UPDATE_TIME (UPDATE_TIME); 
	END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
