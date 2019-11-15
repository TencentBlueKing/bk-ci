USE devops_ci_quality;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_CONTROL_POINT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(64) NOT NULL,
  `TASK_LIST` text NOT NULL,
  `ONLINE` bit(1) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_CONTROL_POINT_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT_METADATA` (
  `METADATA_ID` varchar(128) NOT NULL,
  `METADATA_TYPE` varchar(32) NOT NULL,
  `METADATA_NAME` text NOT NULL,
  `TASK_ID` varchar(64) NOT NULL,
  `ONLINE` bit(1) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL,
  PRIMARY KEY (`METADATA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_CONTROL_POINT_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT_TASK` (
  `ID` varchar(64) NOT NULL,
  `CONTROL_STAGE` varchar(32) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_COUNT_INTERCEPT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_COUNT_INTERCEPT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `DATE` date NOT NULL,
  `COUNT` int(11) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0',
  `RULE_INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID_DATE` (`PROJECT_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_COUNT_PIPELINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_COUNT_PIPELINE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `DATE` date NOT NULL,
  `COUNT` int(11) NOT NULL,
  `LAST_INTERCEPT_TIME` datetime NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID_PIPELINE_ID_DATE` (`PROJECT_ID`,`PIPELINE_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_COUNT_RULE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_COUNT_RULE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `RULE_ID` bigint(20) NOT NULL,
  `DATE` date NOT NULL,
  `COUNT` int(11) NOT NULL,
  `LAST_INTERCEPT_TIME` datetime NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID_RULE_ID_DATE` (`PROJECT_ID`,`RULE_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GROUP` (
  `ID` bigint(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `INNER_USERS` text NOT NULL,
  `INNER_USERS_COUNT` int(11) NOT NULL,
  `OUTER_USERS` text NOT NULL,
  `OUTER_USERS_COUNT` int(11) NOT NULL,
  `REMARK` text,
  `CREATOR` varchar(64) NOT NULL,
  `UPDATOR` varchar(64) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_HISTORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_HISTORY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `RULE_ID` bigint(20) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `BUILD_ID` varchar(34) NOT NULL,
  `RESULT` varchar(34) NOT NULL,
  `INTERCEPT_LIST` text NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PROJECT_NUM` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `RULE_ID` (`RULE_ID`) USING BTREE,
  KEY `PROJECT_ID_RULE_ID` (`PROJECT_ID`,`RULE_ID`) USING BTREE,
  KEY `PROJECT_ID_PIPELINE_ID_BUILD_ID` (`PROJECT_ID`,`PIPELINE_ID`,`BUILD_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_QUALITY_CONTROL_POINT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_CONTROL_POINT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ELEMENT_TYPE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `STAGE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `AVAILABLE_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `DEFAULT_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ENABLE` bit(1) DEFAULT NULL,
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `ELEMENT_TYPE_INDEX` (`ELEMENT_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线控制点表';

-- ----------------------------
-- Table structure for T_QUALITY_HIS_DETAIL_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_HIS_DETAIL_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATA_ID` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `DATA_NAME` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `DATA_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `DATA_DESC` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `DATA_VALUE` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_TYPE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `PIPELINE_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `BUILD_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `BUILD_NO` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `EXTRA` text COLLATE utf8mb4_bin,
  KEY `BUILD_DATA_ID_INDEX` (`DATA_ID`,`BUILD_ID`),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='执行结果详细基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_HIS_ORIGIN_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_HIS_ORIGIN_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `PIPELINE_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `BUILD_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `BUILD_NO` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `RESULT_DATA` text COLLATE utf8mb4_bin,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='执行结果基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_INDICATOR
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_INDICATOR` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ELEMENT_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `EN_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CN_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `METADATA_IDS` text COLLATE utf8mb4_bin,
  `DEFAULT_OPERATION` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `OPERATION_AVAILABLE` text COLLATE utf8mb4_bin,
  `THRESHOLD` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `THRESHOLD_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL,
  `INDICATOR_READ_ONLY` bit(1) DEFAULT NULL,
  `STAGE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `INDICATOR_RANGE` text COLLATE utf8mb4_bin,
  `ENABLE` bit(1) DEFAULT NULL,
  `TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT 'SYSTEM',
  `TAG` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线指标表';

-- ----------------------------
-- Table structure for T_QUALITY_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATA_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `DATA_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_TYPE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `VALUE_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL,
  `EXTRA` text COLLATE utf8mb4_bin,
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_RULE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL,
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL,
  `INDICATOR_RANGE` text COLLATE utf8mb4_bin,
  `CONTROL_POINT` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CONTROL_POINT_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `ENABLE` bit(1) DEFAULT b'1',
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `INTERCEPT_TIMES` int(11) DEFAULT '0',
  `EXECUTE_COUNT` int(11) DEFAULT '0',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Table structure for T_QUALITY_RULE_MAP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_MAP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `RULE_ID` bigint(20) DEFAULT NULL,
  `INDICATOR_IDS` text COLLATE utf8mb4_bin,
  `INDICATOR_OPERATIONS` text COLLATE utf8mb4_bin,
  `INDICATOR_THRESHOLDS` text COLLATE utf8mb4_bin,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `RULE_INDEX` (`RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Table structure for T_QUALITY_RULE_OPERATION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_OPERATION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `RULE_ID` bigint(20) DEFAULT NULL,
  `TYPE` varchar(16) COLLATE utf8mb4_bin DEFAULT NULL,
  `NOTIFY_USER` text COLLATE utf8mb4_bin,
  `NOTIFY_GROUP_ID` text COLLATE utf8mb4_bin,
  `NOTIFY_TYPES` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `AUDIT_USER` text COLLATE utf8mb4_bin,
  `AUDIT_TIMEOUT` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `RULE_ID_INDEX` (`RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Table structure for T_QUALITY_RULE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_TEMPLATE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `TYPE` varchar(16) COLLATE utf8mb4_bin DEFAULT NULL,
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL,
  `STAGE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CONTROL_POINT` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CONTROL_POINT_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `ENABLE` bit(1) DEFAULT b'1',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线模板表';

-- ----------------------------
-- Table structure for T_QUALITY_TEMPLATE_INDICATOR_MAP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_TEMPLATE_INDICATOR_MAP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TEMPLATE_ID` bigint(20) DEFAULT NULL,
  `INDICATOR_ID` bigint(20) DEFAULT NULL,
  `OPERATION` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `THRESHOLD` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='模板-指标关系表';

-- ----------------------------
-- Table structure for T_RULE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_RULE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `REMARK` text,
  `TYPE` varchar(32) NOT NULL,
  `CONTROL_POINT` varchar(32) NOT NULL,
  `TASK_ID` varchar(64) NOT NULL,
  `THRESHOLD` text NOT NULL,
  `INDICATOR_RANGE` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `RANGE_IDENTIFICATION` text NOT NULL,
  `OPERATION` varchar(32) NOT NULL,
  `OPERATION_END_NOTIFY_TYPE` varchar(128) DEFAULT NULL,
  `OPERATION_END_NOTIFY_GROUP` text,
  `OPERATION_END_NOTIFY_USER` text,
  `OPERATION_AUDIT_NOTIFY_USER` text,
  `INTERCEPT_TIMES` int(11) NOT NULL DEFAULT '0',
  `ENABLE` bit(1) NOT NULL,
  `CREATOR` varchar(32) NOT NULL,
  `UPDATOR` varchar(32) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `IS_DELETED` bit(1) NOT NULL,
  `OPERATION_AUDIT_TIMEOUT_MINUTES` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TASK` (
  `ID` varchar(64) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
