USE devops_ci_remotedev;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_remotedev_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_remotedev_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_WORKSPACE'
                    AND COLUMN_NAME = 'WORKSPACE_KIND') THEN
        ALTER TABLE T_WORKSPACE
            ADD COLUMN `WORKSPACE_KIND` varchar(16) NOT NULL DEFAULT ''
            COMMENT '云桌面类型：cvd-personal/cvd-team'
            AFTER `OWNER_TYPE`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;

CALL ci_remotedev_schema_update();

DROP PROCEDURE IF EXISTS ci_remotedev_schema_update;
