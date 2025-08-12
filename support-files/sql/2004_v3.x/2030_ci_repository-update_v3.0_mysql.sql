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
                        AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN'
                        AND COLUMN_NAME = 'UPDATE_TIME') THEN
        ALTER TABLE T_REPOSITORY_GIT_TOKEN
            ADD COLUMN `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN'
                        AND COLUMN_NAME = 'OPERATOR') THEN
    ALTER TABLE T_REPOSITORY_GIT_TOKEN
        ADD COLUMN `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_GITHUB_TOKEN'
                        AND COLUMN_NAME = 'OPERATOR') THEN
    ALTER TABLE T_REPOSITORY_GITHUB_TOKEN
        ADD COLUMN `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_SCM_TOKEN'
                        AND COLUMN_NAME = 'OPERATOR') THEN
    ALTER TABLE T_REPOSITORY_SCM_TOKEN
        ADD COLUMN `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人';
    END IF;

    IF NOT EXISTS(SELECT 1
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_REPOSITORY'
                            AND COLUMN_NAME = 'SCM_CODE') THEN
        ALTER TABLE T_REPOSITORY
            ADD COLUMN `SCM_CODE` varchar(64) default null comment '代码库标识';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_CODE_GIT'
                        AND COLUMN_NAME = 'CREDENTIAL_TYPE') THEN
    ALTER TABLE T_REPOSITORY_CODE_GIT
        ADD COLUMN `CREDENTIAL_TYPE` varchar(64) DEFAULT NULL COMMENT '凭证类型';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_CODE_SVN'
                        AND COLUMN_NAME = 'CREDENTIAL_TYPE') THEN
    ALTER TABLE T_REPOSITORY_CODE_SVN
        ADD COLUMN `CREDENTIAL_TYPE` varchar(64) DEFAULT NULL COMMENT '凭证类型';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
