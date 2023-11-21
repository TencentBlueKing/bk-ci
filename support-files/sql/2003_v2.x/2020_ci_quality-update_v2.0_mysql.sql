USE devops_ci_quality;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_quality_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_quality_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_CONTROL_POINT'
                    AND COLUMN_NAME = 'ATOM_VERSION') THEN
        ALTER TABLE T_QUALITY_CONTROL_POINT MODIFY COLUMN ATOM_VERSION varchar(30) DEFAULT '1.0.0' COMMENT '插件版本';
    END IF;
    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_QUALITY_INDICATOR'
                AND COLUMN_NAME = 'ATOM_VERSION') THEN
        ALTER TABLE T_QUALITY_INDICATOR MODIFY COLUMN ATOM_VERSION varchar(30) DEFAULT '1.0.0' COMMENT '插件版本号';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_quality_schema_update();
