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
        FROM information_schema.statistics
            WHERE TABLE_SCHEMA = db
            AND TABLE_NAME = 'T_REPOSITORY'
            AND INDEX_NAME = 'inx_alias_name') THEN
        ALTER TABLE T_REPOSITORY ADD INDEX inx_alias_name (ALIAS_NAME);
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_repository_schema_update();