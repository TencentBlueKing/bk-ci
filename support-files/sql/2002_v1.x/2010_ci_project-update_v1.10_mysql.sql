USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

	IF NOT EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DATA_SOURCE'
                AND COLUMN_NAME = 'TAG') THEN
        ALTER TABLE T_DATA_SOURCE ADD `TAG` varchar(128) DEFAULT NULL COMMENT '数据源标签';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
