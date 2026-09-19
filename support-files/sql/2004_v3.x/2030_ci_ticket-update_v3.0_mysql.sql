USE devops_ci_ticket;

DROP PROCEDURE IF EXISTS ci_ticket_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_ticket_schema_update()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_CREDENTIAL'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_CREDENTIAL`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_CERT'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_CERT`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_CERT_ENTERPRISE'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_CERT_ENTERPRISE`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'T_CERT_TLS'
          AND COLUMN_NAME = 'AES_KEY_SHA'
    ) THEN
        ALTER TABLE `T_CERT_TLS`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;
END <CI_UBF>
DELIMITER ;

CALL ci_ticket_schema_update();
DROP PROCEDURE IF EXISTS ci_ticket_schema_update;
