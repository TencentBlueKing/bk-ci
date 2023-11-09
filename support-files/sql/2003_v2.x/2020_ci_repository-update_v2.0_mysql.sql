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
                        AND COLUMN_NAME = 'ENABLE_PAC') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `ENABLE_PAC` bit(1) DEFAULT b'0' NOT NULL COMMENT '是否开启pac';
        END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY'
                        AND COLUMN_NAME = 'PAC_SYNC_STATUS') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `PAC_SYNC_STATUS` VARCHAR(10) NULL COMMENT 'pac同步状态';
        END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY'
                        AND COLUMN_NAME = 'PAC_SYNC_CI_DIR_ID') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `PAC_SYNC_CI_DIR_ID` VARCHAR(64) NULL COMMENT 'pac同步的commitId';
        END IF;

        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY'
                        AND COLUMN_NAME = 'PAC_SYNC_TIME') THEN
        ALTER TABLE `T_REPOSITORY`
            ADD COLUMN `PAC_SYNC_TIME` timestamp NULL COMMENT 'pac同步时间';
        END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_repository_schema_update();
