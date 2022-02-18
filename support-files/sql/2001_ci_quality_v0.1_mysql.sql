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
                    AND TABLE_NAME = 'T_QUALITY_CONTROL_POINT'
                    AND COLUMN_NAME = 'TAG') THEN
        ALTER TABLE `T_QUALITY_CONTROL_POINT`
            ADD COLUMN `TAG` VARCHAR(64) NULL COMMENT '标签';
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_CONTROL_POINT'
                        AND COLUMN_NAME = 'TAG'
                        AND COLUMN_TYPE = 'VARCHAR(64)') THEN
        ALTER TABLE T_QUALITY_CONTROL_POINT
            CHANGE `TAG` `TAG` VARCHAR(64) NULL;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_CONTROL_POINT'
                    AND INDEX_NAME = 'ELEMENT_TYPE_INDEX') THEN
        ALTER TABLE `T_QUALITY_CONTROL_POINT`
	        DROP KEY `ELEMENT_TYPE_INDEX` ;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_HISTORY'
                    AND COLUMN_NAME = 'CHECK_TIMES') THEN
        ALTER TABLE `T_HISTORY` ADD COLUMN `CHECK_TIMES` INT DEFAULT 1 COMMENT '第几次检查';
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_HISTORY'
                        AND COLUMN_NAME = 'CHECK_TIMES'
                        AND COLUMN_TYPE = 'INT') THEN
        ALTER TABLE T_HISTORY
            CHANGE `CHECK_TIMES` `CHECK_TIMES` INT DEFAULT 1;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_quality_schema_update();