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
                     AND TABLE_NAME = 'T_STORE_BUILD_INFO'
                     AND INDEX_NAME = 'INX_TSBI_TYPE_ENABLE') THEN
    ALTER TABLE `T_STORE_BUILD_INFO` ADD INDEX `INX_TSBI_TYPE_ENABLE` (`STORE_TYPE`, `ENABLE`);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_LABEL'
                   AND INDEX_NAME = 'uni_inx_name_type') THEN
    ALTER TABLE `T_LABEL` DROP INDEX `uni_inx_name_type`;
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_LABEL'
                   AND INDEX_NAME = 'uni_inx_code_type') THEN
    ALTER TABLE `T_LABEL` DROP INDEX `uni_inx_code_type`;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_LABEL'
                     AND INDEX_NAME = 'UNI_INX_TL_TYPE_NAME') THEN
    ALTER TABLE `T_LABEL` ADD UNIQUE INDEX `UNI_INX_TL_TYPE_NAME` (`TYPE`, `LABEL_NAME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_LABEL'
                     AND INDEX_NAME = 'UNI_INX_TL_TYPE_CODE') THEN
    ALTER TABLE `T_LABEL` ADD UNIQUE INDEX `UNI_INX_TL_TYPE_CODE` (`TYPE`, `LABEL_CODE`);
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_ATOM MODIFY COLUMN VERSION varchar(30)  NOT NULL COMMENT '版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'BRANCH_TEST_FLAG') THEN
        ALTER TABLE T_ATOM ADD BRANCH_TEST_FLAG bit(1) DEFAULT b'0' NULL COMMENT '是否是分支测试版本';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'LATEST_TEST_FLAG') THEN
        ALTER TABLE T_ATOM ADD LATEST_TEST_FLAG bit(1) DEFAULT b'0' NULL COMMENT '是否为最新测试版本原子， TRUE：最新 FALSE：非最新';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_APPROVE'
                     AND INDEX_NAME = 'inx_tsa_store_code') THEN
        ALTER TABLE T_STORE_APPROVE ADD INDEX `inx_tsa_store_code` (`STORE_CODE`);
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_APPROVE'
                     AND INDEX_NAME = 'inx_tsa_type') THEN
        ALTER TABLE T_STORE_APPROVE DROP INDEX `inx_tsa_type`;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_APPROVE'
                     AND INDEX_NAME = 'inx_tsa_applicant') THEN
        ALTER TABLE T_STORE_APPROVE DROP INDEX `inx_tsa_applicant`;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_APPROVE'
                     AND INDEX_NAME = 'inx_tsa_status') THEN
        ALTER TABLE T_STORE_APPROVE DROP INDEX `inx_tsa_status`;
    END IF;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_STATISTICS_DAILY'
                    AND COLUMN_NAME = 'DAILY_ACTIVE_DURATION') THEN
        ALTER TABLE T_STORE_STATISTICS_DAILY ADD COLUMN `DAILY_ACTIVE_DURATION` decimal(15,2) COMMENT '每日活跃时长，单位：小时';
    END IF;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_STATISTICS_TOTAL'
                    AND COLUMN_NAME = 'RECENT_ACTIVE_DURATION') THEN
        ALTER TABLE T_STORE_STATISTICS_TOTAL ADD COLUMN `RECENT_ACTIVE_DURATION` decimal(15,2) COMMENT '总活跃时长，单位：小时';
    END IF;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_STORE_PROJECT_REL ADD COLUMN `VERSION` varchar(256) COMMENT '版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_MEMBER'
                     AND INDEX_NAME = 'UNI_INX_TSM_TYPE_CODE_NAME') THEN
        ALTER TABLE T_STORE_MEMBER ADD INDEX `UNI_INX_TSM_TYPE_CODE_NAME`(`STORE_TYPE`, `STORE_CODE`,`USERNAME`);
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_STORE_MEMBER'
                     AND INDEX_NAME = 'uni_inx_tam_code_name_type') THEN
        ALTER TABLE T_STORE_MEMBER DROP INDEX `uni_inx_tam_code_name_type`;
    END IF;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_PIPELINE_REL'
                    AND COLUMN_NAME = 'PROJECT_CODE') THEN
       ALTER TABLE T_STORE_PIPELINE_REL ADD COLUMN `PROJECT_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '项目代码';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_PROJECT_REL'
                    AND COLUMN_NAME = 'PROJECT_CODE'
                    AND COLUMN_TYPE = 'varchar(32)') THEN
       ALTER TABLE T_STORE_PROJECT_REL MODIFY COLUMN PROJECT_CODE varchar(64) NOT NULL COMMENT '所属项目';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_APPROVE_REL'
                    AND COLUMN_NAME = 'TEST_PROJECT_CODE'
                    AND COLUMN_TYPE = 'varchar(32)') THEN
       ALTER TABLE T_ATOM_APPROVE_REL MODIFY COLUMN TEST_PROJECT_CODE varchar(64) NOT NULL COMMENT '调试项目编码';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
