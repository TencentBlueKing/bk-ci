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
                        AND TABLE_NAME = 'T_PROJECT'
                        AND COLUMN_NAME = 'tenant_english_name') THEN
       ALTER TABLE T_PROJECT
          ADD COLUMN `tenant_english_name` varchar(32) DEFAULT (english_name) COMMENT '租户英文名';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT'
                        AND COLUMN_NAME = 'tenant_id') THEN
       ALTER TABLE T_PROJECT
          ADD COLUMN `tenant_id` varchar(32) DEFAULT 'default' COMMENT '租户ID';
    END IF;

    IF EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT'
                     AND INDEX_NAME = 'project_name') THEN
      ALTER TABLE `T_PROJECT` DROP INDEX `project_name`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT'
                     AND INDEX_NAME = 'project_name_tenant_id') THEN
      ALTER TABLE `T_PROJECT` ADD UNIQUE KEY `project_name_tenant_id` (`project_name`,`tenant_id`) ;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT'
                     AND INDEX_NAME = 'tenant_id_tenant_english_name') THEN
      ALTER TABLE `T_PROJECT` ADD UNIQUE KEY `tenant_id_tenant_english_name` (`tenant_id`,`tenant_english_name`);
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_APPROVEL'
                        AND COLUMN_NAME = 'TENANT_ENGLISH_NAME') THEN
       ALTER TABLE T_PROJECT_APPROVEL
          ADD COLUMN `TENANT_ENGLISH_NAME` varchar(32) DEFAULT (ENGLISH_NAME) COMMENT '租户英文名';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT_APPROVEL'
                        AND COLUMN_NAME = 'TENANT_ID') THEN
       ALTER TABLE T_PROJECT_APPROVEL
          ADD COLUMN `TENANT_ID` varchar(32) DEFAULT 'default' COMMENT '租户ID';
    END IF;

    IF EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT_APPROVEL'
                     AND INDEX_NAME = 'project_name') THEN
      ALTER TABLE `T_PROJECT_APPROVEL` DROP INDEX `project_name`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT_APPROVEL'
                     AND INDEX_NAME = 'project_name_tenant_id') THEN
      ALTER TABLE `T_PROJECT_APPROVEL` ADD UNIQUE KEY `project_name_tenant_id` (`PROJECT_NAME`,`TENANT_ID`) ;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_PROJECT_APPROVEL'
                     AND INDEX_NAME = 'tenant_id_tenant_english_name') THEN
      ALTER TABLE `T_PROJECT_APPROVEL` ADD UNIQUE KEY `tenant_id_tenant_english_name` (`TENANT_ID`,`TENANT_ENGLISH_NAME`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
