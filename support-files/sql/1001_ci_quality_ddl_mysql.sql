USE devops_ci_quality;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_CONTROL_POINT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(64) NOT NULL COMMENT '名称',
  `TASK_LIST` text NOT NULL COMMENT '任务信息列表',
  `ONLINE` bit(1) NOT NULL COMMENT '是否在线',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_CONTROL_POINT_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT_METADATA` (
  `METADATA_ID` varchar(128) NOT NULL COMMENT '元数据ID',
  `METADATA_TYPE` varchar(32) NOT NULL COMMENT '元数据类型',
  `METADATA_NAME` text NOT NULL COMMENT '元数据名称',
  `TASK_ID` varchar(64) NOT NULL COMMENT '任务ID',
  `ONLINE` bit(1) NOT NULL COMMENT '是否在线',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`METADATA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_CONTROL_POINT_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTROL_POINT_TASK` (
  `ID` varchar(64) NOT NULL COMMENT '主键ID',
  `CONTROL_STAGE` varchar(32) NOT NULL COMMENT '原子控制阶段',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_COUNT_INTERCEPT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_COUNT_INTERCEPT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `DATE` date NOT NULL COMMENT '日期',
  `COUNT` int(11) NOT NULL COMMENT '计数',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0' COMMENT '拦截数',
  `RULE_INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0' COMMENT 'RULE_INTERCEPT_COUNT + count)',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID_DATE` (`PROJECT_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_COUNT_PIPELINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_COUNT_PIPELINE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `DATE` date NOT NULL COMMENT '日期',
  `COUNT` int(11) NOT NULL COMMENT '计数',
  `LAST_INTERCEPT_TIME` datetime NOT NULL COMMENT '上次拦截时间',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `INTERCEPT_COUNT` int(11) NOT NULL DEFAULT '0' COMMENT '拦截数',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID_PIPELINE_ID_DATE` (`PROJECT_ID`,`PIPELINE_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_COUNT_RULE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_COUNT_RULE`
(
    `ID`                  bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `PROJECT_ID`          varchar(32) NOT NULL COMMENT '项目ID',
    `RULE_ID`             bigint(20)  NOT NULL COMMENT '规则ID',
    `DATE`                date        NOT NULL COMMENT '日期',
    `COUNT`               int(11)     NOT NULL COMMENT '计数',
    `INTERCEPT_COUNT`     int(11)     NOT NULL DEFAULT '0' COMMENT '拦截数',
    `LAST_INTERCEPT_TIME` datetime    NOT NULL COMMENT '上次拦截时间',
    `CREATE_TIME`         datetime    NOT NULL COMMENT '创建时间',
    `UPDATE_TIME`         datetime    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `PROJECT_ID_RULE_ID_DATE` (`PROJECT_ID`, `RULE_ID`, `DATE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE`
(
    `ID`                      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `NAME`                    varchar(128)          DEFAULT NULL COMMENT '规则名称',
    `DESC`                    varchar(256)          DEFAULT NULL COMMENT '规则描述',
    `INDICATOR_RANGE`         text COMMENT '指标范围',
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
    `QUALITY_RULE_HASH_ID`    varchar(64) DEFAULT NULL COMMENT '质量规则哈希ID',
    `GATEWAY_ID`              varchar(128) NOT NULL DEFAULT '' COMMENT '红线匹配的id',
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT='';

-- ----------------------------
-- Table structure for T_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GROUP` (
  `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `NAME` varchar(64) NOT NULL COMMENT '名称',
  `INNER_USERS` text NOT NULL COMMENT '内部人员',
  `INNER_USERS_COUNT` int(11) NOT NULL COMMENT '内部人员计数',
  `OUTER_USERS` text NOT NULL COMMENT '外部人员',
  `OUTER_USERS_COUNT` int(11) NOT NULL COMMENT '外部人员计数',
  `REMARK` text COMMENT '评论',
  `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
  `UPDATOR` varchar(64) NOT NULL COMMENT '更新人',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_HISTORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_HISTORY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `RULE_ID` bigint(20) NOT NULL COMMENT '规则ID',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `RESULT` varchar(34) NOT NULL COMMENT '',
  `INTERCEPT_LIST` text NOT NULL COMMENT '拦截列表',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `PROJECT_NUM` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目数量',
  `CHECK_TIMES` INT DEFAULT 1 COMMENT '第几次检查',
  PRIMARY KEY (`ID`),
  KEY `RULE_ID` (`RULE_ID`) USING BTREE,
  KEY `PROJECT_ID_RULE_ID` (`PROJECT_ID`,`RULE_ID`) USING BTREE,
  KEY `PROJECT_ID_PIPELINE_ID_BUILD_ID` (`PROJECT_ID`,`PIPELINE_ID`,`BUILD_ID`) USING BTREE,
  KEY `IDX_RULE_PROJECT_NUM` (`RULE_ID`,`PROJECT_ID`,`PROJECT_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_QUALITY_CONTROL_POINT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_CONTROL_POINT`
(
    `ID`                 bigint(20)                   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ELEMENT_TYPE`       varchar(64)        DEFAULT NULL COMMENT '原子的ClassType',
    `NAME`               varchar(64)        DEFAULT NULL COMMENT '控制点名称(原子名称)',
    `STAGE`              varchar(64)        DEFAULT NULL COMMENT '研发阶段',
    `AVAILABLE_POSITION` varchar(64)        DEFAULT NULL COMMENT '支持红线位置(准入-BEFORE, 准出-AFTER)',
    `DEFAULT_POSITION`   varchar(64)        DEFAULT NULL COMMENT '默认红线位置',
    `ENABLE`             bit(1)                                DEFAULT NULL COMMENT '是否启用',
    `CREATE_USER`        varchar(64)        DEFAULT NULL COMMENT '创建用户',
    `UPDATE_USER`        varchar(64)        DEFAULT NULL COMMENT '更新用户',
    `CREATE_TIME`        datetime                              DEFAULT NULL COMMENT '创建时间',
    `UPDATE_TIME`        datetime                              DEFAULT NULL COMMENT '更新时间',
    `ATOM_VERSION`       varchar(16)        DEFAULT '1.0.0' COMMENT '插件版本',
    `TEST_PROJECT`       varchar(64) NOT NULL DEFAULT '' COMMENT '测试的项目',
    `CONTROL_POINT_HASH_ID` varchar(64) DEFAULT NULL COMMENT '哈希ID',
    `TAG` VARCHAR(64) NULL,
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='质量红线控制点表';

-- ----------------------------
-- Table structure for T_QUALITY_HIS_DETAIL_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_HIS_DETAIL_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `DATA_ID` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据ID',
  `DATA_NAME` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据名称',
  `DATA_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据类型',
  `DATA_DESC` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据描述',
  `DATA_VALUE` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据值',
  `ELEMENT_TYPE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '原子的ClassType',
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '工具/原子子类',
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '构建ID',
  `BUILD_NO` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '构建号',
  `CREATE_TIME` bigint(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建时间',
  `EXTRA` text COLLATE utf8mb4_bin COMMENT '额外信息',
  `TASK_ID` varchar(34) DEFAULT NULL COMMENT '任务节点id',
  `TASK_NAME` varchar(128) COMMENT '任务节点名',
  UNIQUE KEY BUILD_ID_DATA_ID_TASK_ID_INDEX (`BUILD_ID`, `DATA_ID`, `TASK_ID`),
  KEY `CREATE_TIME_INX` (`CREATE_TIME`),
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='执行结果详细基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_HIS_ORIGIN_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_HIS_ORIGIN_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '构建ID',
  `BUILD_NO` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '构建号',
  `RESULT_DATA` text COLLATE utf8mb4_bin COMMENT '返回数据',
  `CREATE_TIME` bigint(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建时间',
   KEY `CREATE_TIME_INX` (`CREATE_TIME`),
   PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='执行结果基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_INDICATOR
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_QUALITY_INDICATOR`
(
    `ID`                  bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ELEMENT_TYPE`        varchar(32)            DEFAULT NULL COMMENT '原子的ClassType',
    `ELEMENT_NAME`        varchar(64)            DEFAULT NULL COMMENT '产出原子',
    `ELEMENT_DETAIL`      varchar(64)            DEFAULT NULL COMMENT '工具/原子子类',
    `EN_NAME`             varchar(64)            DEFAULT NULL COMMENT '指标英文名',
    `CN_NAME`             varchar(64)            DEFAULT NULL COMMENT '指标中文名',
    `METADATA_IDS`        text COMMENT '指标所包含基础数据',
    `DEFAULT_OPERATION`   varchar(32)            DEFAULT NULL COMMENT '默认操作',
    `OPERATION_AVAILABLE` text COMMENT '可用操作',
    `THRESHOLD`           varchar(64)            DEFAULT NULL COMMENT '默认阈值',
    `THRESHOLD_TYPE`      varchar(32)            DEFAULT NULL COMMENT '阈值类型',
    `DESC`                varchar(256)           DEFAULT NULL COMMENT '描述',
    `INDICATOR_READ_ONLY` bit(1)                 DEFAULT NULL COMMENT '是否只读',
    `STAGE`               varchar(32)            DEFAULT NULL COMMENT '阶段',
    `INDICATOR_RANGE`     text COMMENT '指标范围',
    `ENABLE`              bit(1)                 DEFAULT NULL COMMENT '是否启用',
    `TYPE`                varchar(32)            DEFAULT 'SYSTEM' COMMENT '指标类型',
    `TAG`                 varchar(32)            DEFAULT NULL COMMENT '指标标签，用于前端区分控制',
    `CREATE_USER`         varchar(64)            DEFAULT NULL COMMENT '创建用户',
    `UPDATE_USER`         varchar(64)            DEFAULT NULL COMMENT '更新用户',
    `CREATE_TIME`         datetime               DEFAULT NULL COMMENT '创建时间',
    `UPDATE_TIME`         datetime               DEFAULT NULL COMMENT '更新时间',
    `ATOM_VERSION`        varchar(16)   NOT NULL DEFAULT '1.0.0' COMMENT '插件版本号',
    `LOG_PROMPT`          varchar(1024) NOT NULL DEFAULT '' COMMENT '日志提示',
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='质量红线指标表';

-- ----------------------------
-- Table structure for T_QUALITY_METADATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_METADATA` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `DATA_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据ID',
  `DATA_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据名称',
  `ELEMENT_TYPE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '原子的ClassType',
  `ELEMENT_NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '产出原子',
  `ELEMENT_DETAIL` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '工具/原子子类',
  `VALUE_TYPE` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'value值前端组件类型',
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '描述',
  `EXTRA` text COLLATE utf8mb4_bin COMMENT '额外信息',
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建者',
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `IDX_DATA_TYPE` (`DATA_ID`,`ELEMENT_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线基础数据表';

-- ----------------------------
-- Table structure for T_QUALITY_RULE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '名称',
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '描述',
  `INDICATOR_RANGE` text COLLATE utf8mb4_bin COMMENT '指标范围',
  `CONTROL_POINT` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '控制点原子类型',
  `CONTROL_POINT_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '控制点红线位置',
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建者',
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ENABLE` bit(1) DEFAULT b'1' COMMENT '是否启用',
  `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '项目ID',
  `INTERCEPT_TIMES` int(11) DEFAULT '0' COMMENT '拦截次数',
  `EXECUTE_COUNT` int(11) DEFAULT '0' COMMENT '执行次数',
  PRIMARY KEY (`ID`),
  KEY `IDX_PROJECT` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='';

-- ----------------------------
-- Table structure for T_QUALITY_RULE_MAP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_MAP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `RULE_ID` bigint(20) DEFAULT NULL COMMENT '规则ID',
  `INDICATOR_IDS` text COLLATE utf8mb4_bin COMMENT '指标类型',
  `INDICATOR_OPERATIONS` text COLLATE utf8mb4_bin COMMENT '指标操作',
  `INDICATOR_THRESHOLDS` text COLLATE utf8mb4_bin COMMENT '指标阈值',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `RULE_INDEX` (`RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='';

-- ----------------------------
-- Table structure for T_QUALITY_RULE_OPERATION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_OPERATION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `RULE_ID` bigint(20) DEFAULT NULL COMMENT '规则ID',
  `TYPE` varchar(16) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '类型',
  `NOTIFY_USER` text COLLATE utf8mb4_bin COMMENT '通知人员',
  `NOTIFY_GROUP_ID` text COLLATE utf8mb4_bin COMMENT '用户组ID',
  `NOTIFY_TYPES` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '通知类型',
  `AUDIT_USER` text COLLATE utf8mb4_bin COMMENT '审核人员',
  `AUDIT_TIMEOUT` int(11) DEFAULT NULL COMMENT '审核超时时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `RULE_ID_INDEX` (`RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='';

-- ----------------------------
-- Table structure for T_QUALITY_RULE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_TEMPLATE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '名称',
  `TYPE` varchar(16) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '类型',
  `DESC` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '描述',
  `STAGE` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '阶段',
  `CONTROL_POINT` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '控制点原子类型',
  `CONTROL_POINT_POSITION` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '控制点红线位置',
  `CREATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建者',
  `UPDATE_USER` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `ENABLE` bit(1) DEFAULT b'1' COMMENT '是否启用',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='质量红线模板表';

-- ----------------------------
-- Table structure for T_QUALITY_TEMPLATE_INDICATOR_MAP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_TEMPLATE_INDICATOR_MAP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `TEMPLATE_ID` bigint(20) DEFAULT NULL COMMENT '模板ID',
  `INDICATOR_ID` bigint(20) DEFAULT NULL COMMENT '指标ID',
  `OPERATION` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '可选操作',
  `THRESHOLD` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '默认阈值',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='模板-指标关系表';

-- ----------------------------
-- Table structure for T_RULE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_RULE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `NAME` varchar(128) NOT NULL COMMENT '名称',
  `REMARK` text COMMENT '评论',
  `TYPE` varchar(32) NOT NULL COMMENT '类型',
  `CONTROL_POINT` varchar(32) NOT NULL COMMENT '控制点原子类型',
  `TASK_ID` varchar(64) NOT NULL COMMENT '任务ID',
  `THRESHOLD` text NOT NULL COMMENT '默认阈值',
  `INDICATOR_RANGE` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '指标范围',
  `RANGE_IDENTIFICATION` text NOT NULL COMMENT 'ANY-项目ID集合, PART_BY_NAME-空集合',
  `OPERATION` varchar(32) NOT NULL COMMENT '可选操作',
  `OPERATION_END_NOTIFY_TYPE` varchar(128) DEFAULT NULL COMMENT '操作结束通知类型',
  `OPERATION_END_NOTIFY_GROUP` text COMMENT '操作结束通知用户组',
  `OPERATION_END_NOTIFY_USER` text COMMENT '操作结束通知用户',
  `OPERATION_AUDIT_NOTIFY_USER` text COMMENT '操作审核通知用户',
  `INTERCEPT_TIMES` int(11) NOT NULL DEFAULT '0' COMMENT '拦截次数',
  `ENABLE` bit(1) NOT NULL COMMENT '是否启用',
  `CREATOR` varchar(32) NOT NULL COMMENT '创建者',
  `UPDATOR` varchar(32) NOT NULL COMMENT '更新人',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETED` bit(1) NOT NULL COMMENT '是否删除 0 可用 1删除',
  `OPERATION_AUDIT_TIMEOUT_MINUTES` int(11) DEFAULT NULL COMMENT '审核超时时间',
  PRIMARY KEY (`ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TASK` (
  `ID` varchar(64) NOT NULL COMMENT '主键ID',
  `NAME` varchar(255) NOT NULL COMMENT '名称',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for T_QUALITY_RULE_BUILD_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_BUILD_HIS` (
   `ID` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
   `PROJECT_ID` VARCHAR(64) COLLATE utf8_bin DEFAULT NULL COMMENT '项目ID',
   `PIPELINE_ID` VARCHAR(40) COLLATE utf8_bin DEFAULT NULL COMMENT '流水线ID',
   `BUILD_ID` VARCHAR(40) COLLATE utf8_bin DEFAULT NULL COMMENT '构建ID',
   `RULE_POS` VARCHAR(8) COLLATE utf8_bin DEFAULT NULL COMMENT '控制点位置',
   `RULE_NAME` VARCHAR(123) COLLATE utf8_bin DEFAULT NULL COMMENT '规则名称',
   `RULE_DESC` VARCHAR(256) COLLATE utf8_bin DEFAULT NULL COMMENT '规则描述',
   `GATEWAY_ID` VARCHAR(128) COLLATE utf8_bin DEFAULT NULL COMMENT '红线匹配的id',
   `PIPELINE_RANGE` TEXT COLLATE utf8_bin DEFAULT NULL COMMENT '生效的流水线id集合',
   `TEMPLATE_RANGE` TEXT COLLATE utf8_bin DEFAULT NULL COMMENT '生效的流水线模板id集合',
   `INDICATOR_IDS` TEXT COLLATE utf8_bin DEFAULT NULL COMMENT '指标类型',
   `INDICATOR_OPERATIONS` TEXT COLLATE utf8_bin DEFAULT NULL COMMENT '指标操作',
   `INDICATOR_THRESHOLDS` TEXT COLLATE utf8_bin DEFAULT NULL COMMENT '指标阈值',
   `OPERATION_LIST` TEXT COLLATE utf8_bin COMMENT '操作清单',
   `QUALITY_RULE_HIS_HASH_ID` varchar(64) DEFAULT NULL COMMENT '质量规则构建历史哈希ID',
   `CREATE_TIME` DATETIME COMMENT '创建时间',
   `CREATE_USER` VARCHAR(32) COMMENT '创建人',
   `STAGE_ID` varchar(40) COMMENT 'stage_id' NOT NULL DEFAULT '1',
   `STATUS` varchar(20) COMMENT '红线状态',
   `GATE_KEEPERS` varchar(1024) COMMENT '红线把关人',
   `TASK_STEPS` text COMMENT '红线指定的任务节点',
   PRIMARY KEY (`ID`),
   KEY project_id_pipeline_id_idx (`PROJECT_ID`,`PIPELINE_ID`),
   KEY create_time_idx (`CREATE_TIME`),
   KEY `IDX_STAGE_ID` (`STAGE_ID`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='';

-- ----------------------------
-- Table structure for T_QUALITY_RULE_BUILD_HIS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_BUILD_HIS_OPERATION`(
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `RULE_ID` bigint(20) NOT NULL COMMENT '规则id',
  `STAGE_ID` varchar(40) COLLATE utf8_bin NOT NULL,
  `GATE_OPT_USER` varchar(32) DEFAULT NULL,
  `GATE_OPT_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `rule_id_idx` (`RULE_ID`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_QUALITY_RULE_REVIEWER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_QUALITY_RULE_REVIEWER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `RULE_ID` bigint(20) NOT NULL COMMENT '规则ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
  `REVIEWER` varchar(32) NOT NULL COMMENT '实际审核人',
  `REVIEW_TIME` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`ID`),
  KEY `PROJECT_ID_PIPELINE_ID_BUILD_ID` (`PROJECT_ID`, `PIPELINE_ID`, `BUILD_ID`) USING BTREE,
  KEY `RULE_ID` (`RULE_ID`) USING BTREE,
  KEY `REVIEW_TIME` (`REVIEW_TIME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红线审核人';
