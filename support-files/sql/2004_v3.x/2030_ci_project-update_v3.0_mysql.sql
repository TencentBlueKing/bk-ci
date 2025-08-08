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
                        AND TABLE_NAME = 'T_SERVICE'
                        AND COLUMN_NAME = 'DOC_URL') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `DOC_URL` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文档链接';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                        AND COLUMN_NAME = 'PROPERTIES') THEN
        ALTER TABLE T_PROJECT_APPROVAL
            ADD COLUMN `PROPERTIES` text null DEFAULT NULL comment '项目其他配置';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TABLE_SHARDING_CONFIG'
                        AND COLUMN_NAME = 'TYPE') THEN
       ALTER TABLE T_TABLE_SHARDING_CONFIG
          ADD COLUMN `TYPE` varchar(32) NOT NULL DEFAULT '' COMMENT '表类型';
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_TABLE_SHARDING_CONFIG'
                   AND INDEX_NAME = 'UNI_INX_TTSC_CLUSTER_MODULE_NAME') THEN
      ALTER TABLE `T_TABLE_SHARDING_CONFIG` DROP INDEX `UNI_INX_TTSC_CLUSTER_MODULE_NAME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_TABLE_SHARDING_CONFIG'
                     AND INDEX_NAME = 'UNI_INX_TTSC_CLUSTER_MODULE_NAME_TYPE') THEN
      ALTER TABLE `T_TABLE_SHARDING_CONFIG` ADD INDEX
         `UNI_INX_TTSC_CLUSTER_MODULE_NAME_TYPE` (`CLUSTER_NAME`,`MODULE_CODE`,`TABLE_NAME`,`TYPE`);
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_OPERATIONAL_PRODUCT'
                        AND COLUMN_NAME = 'ICOS_PRODUCT_CODE') THEN
        ALTER TABLE T_OPERATIONAL_PRODUCT
            ADD COLUMN `ICOS_PRODUCT_CODE` varchar(64) default NULL COMMENT '财务ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_OPERATIONAL_PRODUCT'
                        AND COLUMN_NAME = 'ICOS_PRODUCT_NAME') THEN
        ALTER TABLE T_OPERATIONAL_PRODUCT
            ADD COLUMN `ICOS_PRODUCT_NAME` varchar(64) default NULL COMMENT '财务名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_OPERATIONAL_PRODUCT'
                            AND COLUMN_NAME = 'CROS_CHECK') THEN
        ALTER TABLE T_OPERATIONAL_PRODUCT
            ADD COLUMN `CROS_CHECK` bit(1) DEFAULT NULL COMMENT '是否有效';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
