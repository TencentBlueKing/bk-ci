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
                    AND TABLE_NAME = 'T_REPOSITORY_GITHUB_TOKEN'
                    AND COLUMN_NAME = 'TYPE') THEN
    ALTER TABLE `T_REPOSITORY_GITHUB_TOKEN`
        ADD COLUMN `TYPE` varchar(32) DEFAULT 'GITHUB_APP' COMMENT 'GitHub token类型（GITHUB_APP、OAUTH_APP）';
	alter table T_REPOSITORY_GITHUB_TOKEN drop key USER_ID;
    alter table T_REPOSITORY_GITHUB_TOKEN add constraint USER_ID unique (USER_ID, TYPE);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GITHUB'
                    AND COLUMN_NAME = 'GIT_PROJECT_ID') THEN
    ALTER TABLE `T_REPOSITORY_GITHUB`
        ADD COLUMN `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GIT_CHECK'
                    AND COLUMN_NAME = 'TARGET_BRANCH') THEN
    ALTER TABLE `T_REPOSITORY_GIT_CHECK`
        ADD COLUMN `TARGET_BRANCH` varchar(1024) default '' not null comment '目标分支';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
