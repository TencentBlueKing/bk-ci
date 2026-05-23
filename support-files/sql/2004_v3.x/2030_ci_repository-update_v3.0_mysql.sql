USE devops_ci_repository;

DROP PROCEDURE IF EXISTS ci_repository_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_repository_schema_update()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_REPOSITORY_GIT_TOKEN`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_REPOSITORY_TGIT_TOKEN'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_REPOSITORY_TGIT_TOKEN`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_REPOSITORY_GITHUB_TOKEN'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_REPOSITORY_GITHUB_TOKEN`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;
END <CI_UBF>
DELIMITER ;

CALL ci_repository_schema_update();
DROP PROCEDURE IF EXISTS ci_repository_schema_update;
