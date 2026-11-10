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
                        AND TABLE_NAME = 'T_REPOSITORY_TGIT_TOKEN'
                        AND COLUMN_NAME = 'OPERATOR') THEN
    ALTER TABLE T_REPOSITORY_TGIT_TOKEN
        ADD COLUMN `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人';
    UPDATE T_REPOSITORY_TGIT_TOKEN SET OPERATOR = USER_ID WHERE OPERATOR IS NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.statistics
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_TGIT_TOKEN'
                        AND INDEX_NAME = 'IDX_REPOSITORY_TGIT_TOKEN_OPERATOR') THEN
    ALTER TABLE `T_REPOSITORY_TGIT_TOKEN`
        ADD INDEX `IDX_REPOSITORY_TGIT_TOKEN_OPERATOR`(`OPERATOR`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();
