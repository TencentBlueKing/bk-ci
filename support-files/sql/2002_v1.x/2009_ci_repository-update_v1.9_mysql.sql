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
                    AND COLUMN_NAME = 'REPOSITORY_HASH_ID') THEN
    ALTER TABLE `T_REPOSITORY`
        ADD COLUMN `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '哈希ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_CODE_GIT'
                    AND COLUMN_NAME = 'GIT_PROJECT_ID') THEN
    ALTER TABLE `T_REPOSITORY_CODE_GIT`
        ADD COLUMN `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_CODE_GITLAB'
                    AND COLUMN_NAME = 'GIT_PROJECT_ID') THEN
    ALTER TABLE `T_REPOSITORY_CODE_GITLAB`
        ADD COLUMN `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_COMMIT'
                    AND COLUMN_NAME = 'URL') THEN
    ALTER TABLE `T_REPOSITORY_COMMIT`
        ADD COLUMN `URL` varchar(255) DEFAULT NULL COMMENT '代码库URL';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
