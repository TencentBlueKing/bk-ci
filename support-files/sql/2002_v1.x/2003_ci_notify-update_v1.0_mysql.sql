USE devops_ci_notify;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_notify_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_notify_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
    
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NOTIFY_EMAIL'
                    AND COLUMN_NAME = 'BODY') THEN
        ALTER TABLE T_NOTIFY_EMAIL
            ADD COLUMN `BODY` mediumtext NOT NULL COMMENT '邮件内容';
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NOTIFY_EMAIL'
                        AND COLUMN_NAME = 'BODY'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_NOTIFY_EMAIL
            CHANGE `BODY` `BODY` mediumtext NOT NULL;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_EMAILS_NOTIFY_MESSAGE_TEMPLATE'
                    AND COLUMN_NAME = 'SENDER') THEN
        ALTER TABLE `T_EMAILS_NOTIFY_MESSAGE_TEMPLATE`
            ADD COLUMN `SENDER` VARCHAR(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_RTX_NOTIFY_MESSAGE_TEMPLATE'
                    AND COLUMN_NAME = 'SENDER') THEN
        ALTER TABLE `T_RTX_NOTIFY_MESSAGE_TEMPLATE`
            ADD COLUMN `SENDER` VARCHAR(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_WECHAT_NOTIFY_MESSAGE_TEMPLATE'
                    AND COLUMN_NAME = 'SENDER') THEN
        ALTER TABLE `T_WECHAT_NOTIFY_MESSAGE_TEMPLATE`
            ADD COLUMN `SENDER` VARCHAR(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者';
    END IF;
    
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_notify_schema_update();