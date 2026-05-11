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
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_REPOSITORY_SCM_TOKEN'
                     AND INDEX_NAME = 'IDX_REPOSITORY_SCM_TOKEN_OPERATOR') THEN
    ALTER TABLE `T_REPOSITORY_SCM_TOKEN`
        ADD INDEX `IDX_REPOSITORY_SCM_TOKEN_OPERATOR`(`OPERATOR`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN'
                     AND INDEX_NAME = 'IDX_REPOSITORY_GIT_TOKEN_OPERATOR') THEN
    ALTER TABLE `T_REPOSITORY_GIT_TOKEN`
        ADD INDEX `IDX_REPOSITORY_GIT_TOKEN_OPERATOR`(`OPERATOR`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_REPOSITORY_GITHUB_TOKEN'
                     AND INDEX_NAME = 'IDX_REPOSITORY_GITHUB_TOKEN_OPERATOR') THEN
    ALTER TABLE `T_REPOSITORY_GITHUB_TOKEN`
        ADD INDEX `IDX_REPOSITORY_GITHUB_TOKEN_OPERATOR`(`OPERATOR`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY'
                    AND COLUMN_NAME = 'REPO_RESOURCE_TYPE') THEN
    ALTER TABLE T_REPOSITORY
        ADD COLUMN `REPO_RESOURCE_TYPE` varchar(32) DEFAULT NULL COMMENT '代码库资源类型';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
