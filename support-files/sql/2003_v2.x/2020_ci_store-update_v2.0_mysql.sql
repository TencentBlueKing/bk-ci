USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_ATOM MODIFY COLUMN VERSION varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '版本号';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
