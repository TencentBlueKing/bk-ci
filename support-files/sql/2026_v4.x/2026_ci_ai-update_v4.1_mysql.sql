USE devops_ci_ai;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_ai_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_ai_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AI_USER_LLM_CONFIG'
                    AND COLUMN_NAME = 'AES_KEY_SHA') THEN
        ALTER TABLE `T_AI_USER_LLM_CONFIG`
            ADD COLUMN `AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_ai_schema_update();
