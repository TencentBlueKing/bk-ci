USE devops_ci_dispatch_macos;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_macos_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_macos_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_VIRTUAL_MACHINE_TYPE'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_VIRTUAL_MACHINE_TYPE ADD COLUMN `VERSION` varchar(255) NOT NULL DEFAULT '' COMMENT '系统版本数字' AFTER `SYSTEM_VERSION`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_macos_schema_update();
