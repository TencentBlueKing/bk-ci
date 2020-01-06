USE devops_ci_repository;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_repository_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_repository_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GTI_TOKEN'
                    AND COLUMN_NAME = 'CREATE_TIME') THEN
        ALTER TABLE `devops_repository`.`T_REPOSITORY_GTI_TOKEN`
        	ADD COLUMN `CREATE_TIME` DATETIME   NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'token的创建时间';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();