USE devops_ci_quality;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_quality_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_quality_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_INDICATOR'
                    AND COLUMN_NAME = 'EN_NAME') THEN
    ALTER TABLE `T_QUALITY_INDICATOR`
        ALTER TABLE `T_QUALITY_INDICATOR` CHANGE EN_NAME INDICATOR_CODE varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT '指标代码';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_INDICATOR'
                    AND COLUMN_NAME = 'CN_NAME') THEN
        ALTER TABLE `T_QUALITY_INDICATOR`
            ALTER TABLE `T_QUALITY_INDICATOR` CHANGE CN_NAME `NAME` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL COMMENT '指标名';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
