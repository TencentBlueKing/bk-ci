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
                        AND TABLE_NAME = 'T_QUALITY_RULE'
                        AND COLUMN_NAME = 'QUALITY_RULE_HASH_ID') THEN
    ALTER TABLE `T_QUALITY_RULE`
        ADD COLUMN `QUALITY_RULE_HASH_ID` varchar(64) DEFAULT NULL COMMENT '质量规则哈希ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                        AND COLUMN_NAME = 'QUALITY_RULE_HIS_HASH_ID') THEN
    ALTER TABLE `T_QUALITY_RULE_BUILD_HIS`
        ADD COLUMN `QUALITY_RULE_HIS_HASH_ID` varchar(64) DEFAULT NULL COMMENT '质量规则构建历史哈希ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_CONTROL_POINT'
                        AND COLUMN_NAME = 'CONTROL_POINT_HASH_ID') THEN
    ALTER TABLE `T_QUALITY_CONTROL_POINT`
        ADD COLUMN `CONTROL_POINT_HASH_ID` varchar(64) DEFAULT NULL COMMENT '哈希ID';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_quality_schema_update();
