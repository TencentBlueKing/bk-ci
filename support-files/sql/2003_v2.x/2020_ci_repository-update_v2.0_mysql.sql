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

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_repository_schema_update();