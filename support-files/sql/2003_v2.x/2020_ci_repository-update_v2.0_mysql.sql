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
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY'
                    AND COLUMN_NAME = 'UPDATED_USER') THEN
    ALTER TABLE `T_REPOSITORY`
        ADD COLUMN `UPDATED_USER` varchar(64) NULL DEFAULT NULL COMMENT '代码库最近修改人';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY'
                    AND COLUMN_NAME = 'ATOM') THEN
    ALTER TABLE `T_REPOSITORY`
        ADD COLUMN `ATOM` bit(1) DEFAULT b'0' COMMENT '是否为插件库(插件库不得修改和删除)';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_PIPELINE_REF'
                    AND COLUMN_NAME = 'CHANNEL') THEN
    ALTER TABLE `T_REPOSITORY_PIPELINE_REF`
        ADD COLUMN `CHANNEL` varchar(32) DEFAULT 'BS' COMMENT '流水线渠道';
    END IF;


    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY'
                        AND COLUMN_NAME = 'ENABLE_PAC') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `ENABLE_PAC` bit(1) DEFAULT b'0' NOT NULL COMMENT '是否开启pac';
        END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY'
                        AND COLUMN_NAME = 'YAML_SYNC_STATUS') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `YAML_SYNC_STATUS` VARCHAR(10) NULL COMMENT 'pac同步状态';
        END IF;

        IF NOT EXISTS(SELECT 1
                           FROM information_schema.statistics
                           WHERE TABLE_SCHEMA = db
                             AND TABLE_NAME = 'T_REPOSITORY_CODE_GIT'
                             AND INDEX_NAME = 'IDX_GIT_PROJECT_ID') THEN
            ALTER TABLE `T_REPOSITORY_CODE_GIT`
                ADD INDEX `IDX_GIT_PROJECT_ID`(`GIT_PROJECT_ID`);
        END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_repository_schema_update();
