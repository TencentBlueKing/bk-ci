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
                    AND COLUMN_NAME = 'AUTH_SECRECY') THEN
    ALTER TABLE `T_PROJECT`
        ADD COLUMN `AUTH_SECRECY` int(10) DEFAULT b'0' COMMENT '项目性质,0-公开,1-保密,2-机密';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'SUBJECT_SCOPES') THEN
    ALTER TABLE `T_PROJECT`
        ADD COLUMN `SUBJECT_SCOPES` text DEFAULT NULL COMMENT '最大可授权人员范围';
    END IF;

     IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'PROJECT_TYPE') THEN
    ALTER TABLE `T_PROJECT_APPROVAL`
        ADD COLUMN `PROJECT_TYPE` int(10) comment '项目类型';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
