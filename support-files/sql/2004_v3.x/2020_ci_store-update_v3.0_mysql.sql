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
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                     AND INDEX_NAME = 'UNI_INX_TSPR_STORE_PROJECT_TYPE_INSTANCE_CREATOR') THEN
    ALTER TABLE `T_STORE_PROJECT_REL` ADD UNIQUE INDEX
        `UNI_INX_TSPR_STORE_PROJECT_TYPE_INSTANCE_CREATOR` (`STORE_TYPE`,`STORE_CODE`,`PROJECT_CODE`,`TYPE`,`INSTANCE_ID`,`CREATOR`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                     AND INDEX_NAME = 'INX_TSPR_TYPE_PROJECT_TYPE_INSTANCE') THEN
    ALTER TABLE `T_STORE_PROJECT_REL` ADD INDEX
        `INX_TSPR_TYPE_PROJECT_TYPE_INSTANCE` (`STORE_TYPE`,`PROJECT_CODE`,`TYPE`,`INSTANCE_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                     AND INDEX_NAME = 'INX_TSPR_CODE_TYPE') THEN
    ALTER TABLE `T_STORE_PROJECT_REL` ADD INDEX
         `INX_TSPR_CODE_TYPE` (`STORE_CODE`,`STORE_TYPE`,`TYPE`);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                   AND INDEX_NAME = 'uni_inx_tspr_code_type') THEN
    ALTER TABLE `T_STORE_PROJECT_REL` DROP INDEX `uni_inx_tspr_code_type`;
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                   AND INDEX_NAME = 'inx_tpapr_project_code') THEN
    ALTER TABLE `T_STORE_PROJECT_REL` DROP INDEX `inx_tpapr_project_code`;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                     AND INDEX_NAME = 'inx_tspr_type') THEN
        ALTER TABLE T_STORE_PROJECT_REL DROP INDEX `inx_tspr_type`;
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_BASE'
                AND COLUMN_NAME = 'BUS_NUM') THEN
        ALTER TABLE T_STORE_BASE ADD `BUS_NUM` bigint(20) DEFAULT NULL COMMENT '业务序号';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_BASE'
                     AND INDEX_NAME = 'INX_TSB_TYPE_CODE_NUM') THEN
     ALTER TABLE `T_STORE_BASE` ADD INDEX `INX_TSB_TYPE_CODE_NUM` (`STORE_TYPE`,`STORE_CODE`,`BUS_NUM`);
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                AND COLUMN_NAME = 'INSTANCE_NAME') THEN
     ALTER TABLE T_STORE_PROJECT_REL ADD `INSTANCE_NAME` varchar(256) DEFAULT NULL COMMENT '实例名称';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_BASE_FEATURE'
                AND COLUMN_NAME = 'SHOW_FLAG') THEN
     ALTER TABLE T_STORE_BASE_FEATURE ADD `SHOW_FLAG` bit(1) DEFAULT b'1' COMMENT '是否在首页展示， TRUE：是 FALSE：不是';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
