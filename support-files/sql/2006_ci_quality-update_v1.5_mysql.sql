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
                        AND TABLE_NAME = 'T_QUALITY_HIS_ORIGIN_METADATA'
                        AND COLUMN_NAME = 'CREATE_TIME') THEN
        ALTER TABLE T_QUALITY_HIS_ORIGIN_METADATA ADD COLUMN `CREATE_TIME` BIGINT(20) COMMENT '创建时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                        AND COLUMN_NAME = 'CREATE_TIME') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA ADD COLUMN `CREATE_TIME` BIGINT(20) COMMENT '创建时间';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                        AND COLUMN_NAME = 'TASK_ID') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA ADD COLUMN `TASK_ID` varchar(34) DEFAULT NULL COMMENT '任务节点id';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                        AND COLUMN_NAME = 'TASK_NAME') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA ADD COLUMN `TASK_NAME` varchar(128) COMMENT '任务节点名';
    END IF;
	
	IF EXISTS(SELECT 1
              FROM information_schema.STATISTICS TC
              WHERE TC.TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                AND TC.INDEX_NAME = 'BUILD_ID_DATA_ID_INDEX') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA
            DROP INDEX BUILD_ID_DATA_ID_INDEX;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLE_CONSTRAINTS TC
                  WHERE TC.TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                    AND TC.CONSTRAINT_TYPE = 'UNIQUE'
                    AND TC.CONSTRAINT_NAME = 'BUILD_ID_DATA_ID_TASK_ID_INDEX') THEN
        ALTER TABLE `T_QUALITY_HIS_DETAIL_METADATA`
            ADD UNIQUE INDEX BUILD_ID_DATA_ID_TASK_ID_INDEX (`BUILD_ID`, `DATA_ID`, `TASK_ID`);
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.STATISTICS TC
              WHERE TC.TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                AND TC.INDEX_NAME = 'BUILD_DATA_ID_INDEX') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA
            DROP INDEX BUILD_DATA_ID_INDEX;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.STATISTICS TC
              WHERE TC.TABLE_NAME = 'T_QUALITY_HIS_DETAIL_METADATA'
                AND TC.INDEX_NAME = 'BUILD_ID_INDEX') THEN
        ALTER TABLE T_QUALITY_HIS_DETAIL_METADATA
            DROP INDEX BUILD_ID_INDEX;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                        AND COLUMN_NAME = 'STAGE_ID') THEN
        ALTER TABLE T_QUALITY_RULE_BUILD_HIS ADD COLUMN `STAGE_ID` varchar(40) COMMENT 'stage_id' NOT NULL DEFAULT '1';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                        AND COLUMN_NAME = 'STATUS') THEN
        ALTER TABLE T_QUALITY_RULE_BUILD_HIS ADD COLUMN `STATUS` varchar(20) COMMENT '红线状态';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                        AND COLUMN_NAME = 'GATE_KEEPERS') THEN
        ALTER TABLE T_QUALITY_RULE_BUILD_HIS ADD COLUMN `GATE_KEEPERS` varchar(1024) COMMENT '红线把关人';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                    AND INDEX_NAME = 'IDX_STAGE_ID') THEN
        ALTER TABLE `T_QUALITY_RULE_BUILD_HIS`
            ADD INDEX `IDX_STAGE_ID` (`STAGE_ID`);
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_QUALITY_RULE_BUILD_HIS'
                        AND COLUMN_NAME = 'TASK_STEPS') THEN
        ALTER TABLE T_QUALITY_RULE_BUILD_HIS ADD COLUMN `TASK_STEPS` text COMMENT '红线指定的任务节点';
    END IF;
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_quality_schema_update();
