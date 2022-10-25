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
                    AND TABLE_NAME = 'T_REPOSITORY'
                    AND COLUMN_NAME = 'REPOSITORY_HASH_ID') THEN
    ALTER TABLE `T_REPOSITORY`
        ADD COLUMN `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '哈希ID';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
