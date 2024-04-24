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

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE'
                    AND COLUMN_NAME = 'cluster_type') THEN
    ALTER TABLE `T_SERVICE`
        ADD COLUMN `cluster_type` VARCHAR(32) NOT NULL DEFAULT '' comment '集群类型';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'product_id') THEN
    ALTER TABLE `T_PROJECT`
        ADD COLUMN `product_id` int(10) DEFAULT NULL comment '运营产品ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'PRODUCT_ID') THEN
    ALTER TABLE `T_PROJECT_APPROVAL`
        ADD COLUMN `PRODUCT_ID` int(10) DEFAULT NULL comment '运营产品ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'PRODUCT_NAME') THEN
    ALTER TABLE `T_PROJECT_APPROVAL`
        ADD COLUMN `PRODUCT_NAME` VARCHAR(64) DEFAULT NULL comment '运营产品名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DATA_SOURCE'
                    AND COLUMN_NAME = 'TYPE') THEN
    ALTER TABLE `T_DATA_SOURCE`
       ADD COLUMN `TYPE` varchar(32) NOT NULL DEFAULT 'DB' COMMENT '数据库类型，DB:普通数据库，ARCHIVE_DB:归档数据库';
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_DATA_SOURCE'
                   AND INDEX_NAME = 'uni_inx_tds_module_name') THEN
    ALTER TABLE `T_DATA_SOURCE` DROP INDEX `uni_inx_tds_module_name`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_DATA_SOURCE'
                     AND INDEX_NAME = 'UNI_INX_TDS_CLUSTER_MODULE_TYPE_NAME') THEN
    ALTER TABLE `T_DATA_SOURCE`
      ADD UNIQUE INDEX `UNI_INX_TDS_CLUSTER_MODULE_TYPE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TYPE`, `DATA_SOURCE_NAME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_DATA_MIGRATE_HISTORY'
                    AND COLUMN_NAME = 'PIPELINE_ID') THEN
    ALTER TABLE `T_PROJECT_DATA_MIGRATE_HISTORY`
      ADD COLUMN `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '流水线ID';
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_PROJECT_DATA_MIGRATE_HISTORY'
                   AND INDEX_NAME = 'INX_TPDMH_PROJECT_MODULE_TAG_NAME') THEN
    ALTER TABLE `T_PROJECT_DATA_MIGRATE_HISTORY` DROP INDEX `INX_TPDMH_PROJECT_MODULE_TAG_NAME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT_DATA_MIGRATE_HISTORY'
                     AND INDEX_NAME = 'INX_TPDMH_MODULE_PROJECT_PIPELINE_NAME') THEN
    ALTER TABLE `T_PROJECT_DATA_MIGRATE_HISTORY`
      ADD UNIQUE INDEX `INX_TPDMH_MODULE_PROJECT_PIPELINE_NAME` (`MODULE_CODE`,`PROJECT_ID`,`PIPELINE_ID`, `TARGET_CLUSTER_NAME`, `TARGET_DATA_SOURCE_NAME`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'business_line_id') THEN
    ALTER TABLE `T_PROJECT`
        ADD COLUMN `business_line_id` bigint(20) DEFAULT NULL COMMENT '业务线ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'business_line_name') THEN
    ALTER TABLE `T_PROJECT`
        ADD COLUMN `business_line_name` varchar(255) DEFAULT NULL COMMENT '业务线名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'business_line_id') THEN
    ALTER TABLE `T_PROJECT_APPROVAL`
        ADD COLUMN `BUSINESS_LINE_ID` bigint(20) DEFAULT NULL COMMENT '业务线ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'BUSINESS_LINE_NAME') THEN
    ALTER TABLE `T_PROJECT_APPROVAL`
        ADD COLUMN `BUSINESS_LINE_NAME` varchar(255) DEFAULT NULL COMMENT '业务线名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_USER'
                    AND COLUMN_NAME = 'BUSINESS_LINE_ID') THEN
        ALTER TABLE T_USER
            ADD COLUMN `BUSINESS_LINE_ID` bigint(20) DEFAULT NULL COMMENT '业务线ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_USER'
                    AND COLUMN_NAME = 'BUSINESS_LINE_NAME') THEN
        ALTER TABLE T_USER
            ADD COLUMN `BUSINESS_LINE_NAME` varchar(255) DEFAULT NULL COMMENT '业务线名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND INDEX_NAME = 'PRODUCT_ID_IDX') THEN
        ALTER TABLE T_PROJECT ADD INDEX `PRODUCT_ID_IDX` (`PRODUCT_ID`);
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
