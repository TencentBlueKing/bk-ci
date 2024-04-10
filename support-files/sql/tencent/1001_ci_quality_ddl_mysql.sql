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
CREATE TABLE IF NOT EXISTS `T_COUNT_RULE`
(
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE`
(
    `ID`                      bigint(20)   NOT NULL AUTO_INCREMENT,
    `NAME`                    varchar(128)          DEFAULT NULL COMMENT '规则名称',
    `DESC`                    varchar(256)          DEFAULT NULL COMMENT '规则描述',
    `INDICATOR_RANGE`         text,
    `CONTROL_POINT`           varchar(64)           DEFAULT NULL COMMENT '控制点原子类型',
    `CONTROL_POINT_POSITION`  varchar(64)           DEFAULT NULL COMMENT '控制点红线位置',
    `CREATE_USER`             varchar(64)           DEFAULT NULL COMMENT '创建用户',
    `UPDATE_USER`             varchar(64)           DEFAULT NULL COMMENT '更新用户',
    `CREATE_TIME`             datetime              DEFAULT NULL COMMENT '创建时间',
    `UPDATE_TIME`             datetime              DEFAULT NULL COMMENT '更新时间',
    `ENABLE`                  bit(1)                DEFAULT b'1' COMMENT '是否启用',
    `PROJECT_ID`              varchar(64)           DEFAULT NULL COMMENT '项目id',
    `INTERCEPT_TIMES`         int(11)               DEFAULT '0' COMMENT '拦截次数',
    `EXECUTE_COUNT`           int(11)               DEFAULT '0' COMMENT '生效流水线执行数',
    `PIPELINE_TEMPLATE_RANGE` text COMMENT '流水线模板生效范围',
    `GATEWAY_ID`              varchar(128) NOT NULL DEFAULT '',
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

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

CREATE TABLE IF NOT EXISTS `T_QUALITY_CONTROL_POINT`
(
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ELEMENT_TYPE` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '原子的ClassType',
  `NAME` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '控制点名称(原子名称)',
  `STAGE` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '研发阶段',
  `AVAILABLE_POSITION` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '支持红线位置(准入-BEFORE, 准出-AFTER)',
  `DEFAULT_POSITION` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '默认红线位置',
  `ENABLE` bit(1) DEFAULT NULL COMMENT '是否启用',
  `CREATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '创建用户',
  `UPDATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '更新用户',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ATOM_VERSION` varchar(16) COLLATE utf8_bin DEFAULT '1.0.0' COMMENT '插件版本',
  `TEST_PROJECT` varchar(64) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '测试的项目',
  `TAG` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `ELEMENT_TYPE_INDEX` (`ELEMENT_TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='质量红线控制点表';

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
CREATE TABLE IF NOT EXISTS `T_QUALITY_INDICATOR`
(
      `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ELEMENT_TYPE` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '原子的ClassType',
  `ELEMENT_NAME` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '产出原子',
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '工具/原子子类',
  `EN_NAME` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '指标英文名',
  `CN_NAME` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '指标中文名',
  `METADATA_IDS` text COLLATE utf8_bin COMMENT '指标所包含基础数据',
  `DEFAULT_OPERATION` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '默认操作',
  `OPERATION_AVAILABLE` text COLLATE utf8_bin COMMENT '可用操作',
  `THRESHOLD` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '默认阈值',
  `THRESHOLD_TYPE` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '阈值类型',
  `DESC` varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '描述',
  `INDICATOR_READ_ONLY` bit(1) DEFAULT NULL COMMENT '是否可修改',
  `STAGE` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '阶段',
  `INDICATOR_RANGE` text COLLATE utf8_bin COMMENT '可见项目范围',
  `ENABLE` bit(1) DEFAULT NULL COMMENT '是否启用',
  `TYPE` varchar(32) COLLATE utf8_bin DEFAULT 'SYSTEM' COMMENT '指标类型',
  `TAG` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '指标标签，用于前端区分控制',
  `CREATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '创建用户',
  `UPDATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '更新用户',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ATOM_VERSION` varchar(16) COLLATE utf8_bin NOT NULL DEFAULT '1.0.0' COMMENT '插件版本号',
  `LOG_PROMPT` varchar(1024) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '用户自定义提示日志',
  PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='质量红线指标表';

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
  `NAME` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '规则名称',
  `DESC` varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '规则描述',
  `INDICATOR_RANGE` text COLLATE utf8_bin COMMENT 'ç”Ÿæ•ˆèŒƒå›´',
  `CONTROL_POINT` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '控制点原子类型',
  `CONTROL_POINT_POSITION` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '控制点红线位置',
  `CREATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '创建用户',
  `UPDATE_USER` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '更新用户',
  `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ENABLE` bit(1) DEFAULT b'1' COMMENT '是否启用',
  `PROJECT_ID` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '项目id',
  `INTERCEPT_TIMES` int(11) DEFAULT '0' COMMENT '拦截次数',
  `EXECUTE_COUNT` int(11) DEFAULT '0' COMMENT '生效流水线执行数',
  `PIPELINE_TEMPLATE_RANGE` text COLLATE utf8_bin COMMENT '流水线模板生效范围',
  `GATEWAY_ID` varchar(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`ID`),
  KEY `idx_project` (`PROJECT_ID`)
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
