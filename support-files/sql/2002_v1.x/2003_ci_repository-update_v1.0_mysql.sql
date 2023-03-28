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
            AND TABLE_NAME = 'T_REPOSITORY'
            AND INDEX_NAME = 'inx_alias_name') THEN
        ALTER TABLE T_REPOSITORY ADD INDEX inx_alias_name (ALIAS_NAME);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_COMMIT'
                    AND INDEX_NAME = 'IDX_BUILD_ID_TIME') THEN
        ALTER TABLE T_REPOSITORY_COMMIT ADD INDEX `IDX_BUILD_ID_TIME` (`BUILD_ID`, `COMMIT_TIME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_COMMIT'
                    AND INDEX_NAME = 'IDX_PIPE_ELEMENT_REPO_TIME') THEN
        ALTER TABLE T_REPOSITORY_COMMIT ADD INDEX `IDX_PIPE_ELEMENT_REPO_TIME` (`PIPELINE_ID`, `ELEMENT_ID`, `REPO_ID`, `COMMIT_TIME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_COMMIT'
                    AND INDEX_NAME = 'IDX_PIPE_ELEMENT_NAME_REPO_TIME') THEN
        ALTER TABLE T_REPOSITORY_COMMIT ADD INDEX `IDX_PIPE_ELEMENT_NAME_REPO_TIME` (`PIPELINE_ID`, `ELEMENT_ID`, `REPO_NAME`, `COMMIT_TIME`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_repository_schema_update();