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
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'HIDDEN') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `HIDDEN` bit(1)
            DEFAULT b'0' COMMENT '是否隐藏';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;
