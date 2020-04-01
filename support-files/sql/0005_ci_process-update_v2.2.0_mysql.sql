USE devops_ci_process;
SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for T_PIPELINE_STAGE_TAG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PIPELINE_STAGE_TAG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STAGE_TAG_NAME` varchar(45) NOT NULL DEFAULT '' COMMENT '阶段标签名称',
  `WEIGHT` int(11) NOT NULL DEFAULT '0' COMMENT '阶段标签权值',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'STAGE_STATUS') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `STAGE_STATUS` text COLLATE utf8mb4_bin DEFAULT NULL COMMENT '流水线各阶段状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_STAGE'
                    AND COLUMN_NAME = 'CONDITIONS') THEN
        ALTER TABLE T_PIPELINE_BUILD_STAGE
            ADD COLUMN `CONDITIONS` mediumtext COLLATE utf8mb4_bin;
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
