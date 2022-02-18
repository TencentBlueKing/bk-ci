USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PROJECT'
                        AND COLUMN_NAME = 'relation_id') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `relation_id` VARCHAR(32) COMMENT '关联系统Id';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SERVICE'
                        AND COLUMN_NAME = 'new_window') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `new_window` bit(1) DEFAULT FALSE COMMENT '是否打开新标签页';
    END IF;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SERVICE'
                        AND COLUMN_NAME = 'new_windowUrl') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `new_windowUrl` varchar(200) DEFAULT '' COMMENT '新标签页地址';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
