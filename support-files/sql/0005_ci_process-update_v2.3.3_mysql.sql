USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
        IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'PROJECT_ID_PIPELINE_ID_BUILD_ID_ELEMENT_ID') THEN
        ALTER TABLE T_REPORT DROP INDEX `PROJECT_ID_PIPELINE_ID_BUILD_ID_ELEMENT_ID`;
		END IF;

        IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'inx_tr_project_id') THEN
			ALTER TABLE T_REPORT ADD INDEX `inx_tr_project_id` (`PROJECT_ID`);
        END IF;
		
		IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'inx_tr_pipeline_id') THEN
			ALTER TABLE T_REPORT ADD INDEX `inx_tr_pipeline_id` (`PIPELINE_ID`);
        END IF;
		
		IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'inx_tr_build_id') THEN
			ALTER TABLE T_REPORT ADD INDEX `inx_tr_build_id` (`BUILD_ID`);
        END IF;
		
		IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND INDEX_NAME = 'inx_tr_element_id') THEN
			ALTER TABLE T_REPORT ADD INDEX `inx_tr_element_id` (`ELEMENT_ID`);
        END IF;

    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
