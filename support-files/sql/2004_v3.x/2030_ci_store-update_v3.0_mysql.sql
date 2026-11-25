USE devops_ci_store;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_store_schema_update()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_STORE_SENSITIVE_CONF'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_STORE_SENSITIVE_CONF`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_STORE_ENV_VAR'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_STORE_ENV_VAR`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;
END <CI_UBF>
DELIMITER ;

CALL ci_store_schema_update();
DROP PROCEDURE IF EXISTS ci_store_schema_update;
