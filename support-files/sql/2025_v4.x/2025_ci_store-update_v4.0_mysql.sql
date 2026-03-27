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
                AND TABLE_NAME = 'T_STORE_VERSION_LOG'
                AND COLUMN_NAME = 'PACKAGE_SIZE') THEN
     ALTER TABLE T_STORE_VERSION_LOG ADD `PACKAGE_SIZE` varchar(1024) CHARACTER SET utf8 DEFAULT NULL COMMENT '版本包大小';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_VERSION_LOG'
                    AND COLUMN_NAME = 'PACKAGE_SIZE') THEN
     ALTER TABLE T_ATOM_VERSION_LOG ADD COLUMN `PACKAGE_SIZE` VARCHAR(1024) DEFAULT NULL COMMENT '版本包大小';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
