USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_BASE_ENV'
                AND COLUMN_NAME = 'SHA256_CONTENT') THEN
     ALTER TABLE T_STORE_BASE_ENV ADD `SHA256_CONTENT` varchar(1024) DEFAULT NULL COMMENT 'SHA256签名串';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_BASE'
                AND COLUMN_NAME = 'OWNER_STORE_CODE') THEN
    ALTER TABLE T_STORE_BASE ADD `OWNER_STORE_CODE` varchar(64) DEFAULT NULL COMMENT '归属应用标识';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
