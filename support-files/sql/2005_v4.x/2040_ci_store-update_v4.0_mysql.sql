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
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'TENANT_ID') THEN
        ALTER TABLE T_ATOM ADD `TENANT_ID` varchar(32) DEFAULT 'default' COMMENT '租户ID';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_TEMPLATE'
                AND COLUMN_NAME = 'TENANT_ID') THEN
        ALTER TABLE T_TEMPLATE ADD `TENANT_ID` varchar(32) DEFAULT 'default' COMMENT '租户ID';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_IMAGE'
                AND COLUMN_NAME = 'TENANT_ID') THEN
        ALTER TABLE T_IMAGE ADD `TENANT_ID` varchar(32) DEFAULT 'default' COMMENT '租户ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_ATOM'
                     AND INDEX_NAME = 'inx_ta_tenant_id') THEN
    ALTER TABLE `T_ATOM` ADD UNIQUE INDEX
        `inx_ta_tenant_id` (`TENANT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_TEMPLATE'
                     AND INDEX_NAME = 'inx_tt_tenant_id') THEN
    ALTER TABLE `T_TEMPLATE` ADD UNIQUE INDEX
        `inx_tt_tenant_id` (`TENANT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_IMAGE'
                     AND INDEX_NAME = 'inx_ti_tenant_id') THEN
    ALTER TABLE `T_IMAGE` ADD UNIQUE INDEX
        `inx_ta_tenant_id` (`TENANT_ID`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
