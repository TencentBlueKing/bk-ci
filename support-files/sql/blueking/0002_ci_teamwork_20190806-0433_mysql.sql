USE devops_ci_teamwork;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_BOARD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_BOARD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) DEFAULT NULL,
  `SELECT_ID` bigint(20) DEFAULT NULL,
  `DEFAULT_BOARD` bit(1) DEFAULT b'0',
  `ENABLE_BACKLOG` bit(1) DEFAULT b'0',
  `BACKLOG_STATE` varchar(255) DEFAULT NULL,
  `CONSTRAINT` varchar(255) DEFAULT NULL,
  `GROUP_FIELD_NAME` char(255) DEFAULT NULL,
  `TYPE` varchar(64) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_BOARD_COLUMN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_BOARD_COLUMN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BOARD_ID` bigint(20) DEFAULT NULL,
  `NAME` char(255) DEFAULT NULL,
  `STATE` char(255) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `MAX` int(11) DEFAULT NULL,
  `MIN` int(11) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  `COLOR` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `DESC` text,
  `DEFAULT_FLOW` bit(1) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_flow_project_id_index` (`PROJECT_ID`) USING BTREE,
  KEY `t_flow_is_delete_index` (`IS_DELETE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW_CHANGE_RECORD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW_CHANGE_RECORD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FLOW_ID` bigint(20) DEFAULT NULL,
  `CHANGE_ITEM` char(255) DEFAULT NULL,
  `BEFORE_VALUE` text,
  `AFTER_VALUE` text,
  `SIMPLE_CONTENT` text,
  `DETAIL_CONTENT` text,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_flow_change_record_flow_id_index` (`FLOW_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW_DIRECTION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW_DIRECTION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FLOW_ID` bigint(20) DEFAULT NULL,
  `NODE_ID` bigint(20) DEFAULT NULL,
  `NEXT_NODE_ID` bigint(20) DEFAULT NULL,
  `PROJECT_ID` varchar(128) DEFAULT NULL,
  `CREATED_USER` varchar(255) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(128) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_flow_direction_flow_id_index` (`FLOW_ID`) USING BTREE,
  KEY `t_flow_direction_node_id_index` (`NODE_ID`) USING BTREE,
  KEY `t_flow_direction_next_node_id_index` (`NEXT_NODE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW_DIRECTION_FIELD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW_DIRECTION_FIELD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FIELD_ID` bigint(20) DEFAULT NULL,
  `FLOW_DIRECTION_ID` bigint(20) DEFAULT NULL,
  `REQUIRED` bit(1) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_flow_direction_field_field_id_index` (`FIELD_ID`) USING BTREE,
  KEY `t_flow_direction_field_flow_direction_id_index` (`FLOW_DIRECTION_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW_NODE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW_NODE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `DESC` text,
  `CATEGORY` varchar(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_FLOW_NODE_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FLOW_NODE_REL` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FLOW_ID` bigint(20) DEFAULT NULL,
  `FLOW_NODE_ID` bigint(20) DEFAULT NULL,
  `LOCALTION` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PARENT_ID` bigint(20) DEFAULT NULL,
  `NUMBER` char(255) DEFAULT NULL,
  `TITLE` char(255) DEFAULT NULL,
  `ISSUE_TYPE_ID` bigint(20) DEFAULT NULL,
  `PRIORITY` char(255) DEFAULT NULL,
  `STATE` bigint(20) DEFAULT NULL,
  `FILE_ID` char(255) DEFAULT NULL,
  `DESC` text,
  `SORT` int(11) DEFAULT '32767',
  `ISSUE_TYPE_CLASSIFY` char(255) DEFAULT NULL,
  `COLOR` varchar(30) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  `CLOSE_REASON` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_issue_number_index` (`NUMBER`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_CHANGE_RECORD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_CHANGE_RECORD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `BEFORE_VALUE` text,
  `AFTER_VALUE` text,
  `CHANGE_FIELD` char(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_COMMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_COMMENT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_DIRECTION_ID` bigint(20) DEFAULT NULL,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `COMMENT` text,
  `PARENT_ID` bigint(20) DEFAULT NULL,
  `PATH` text,
  `DEEP` int(2) DEFAULT NULL,
  `CREATED_USER` varchar(255) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(255) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT NULL,
  `PROJECT_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='issue评论';

-- ----------------------------
-- Table structure for T_ISSUE_DIRECTION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_DIRECTION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `NODE_ID` bigint(20) DEFAULT NULL,
  `NEXT_NODE_ID` bigint(20) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='issue流转实例';

-- ----------------------------
-- Table structure for T_ISSUE_DIRECTION_FIELD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_DIRECTION_FIELD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_DIRECTION_ID` bigint(20) DEFAULT NULL,
  `FIELD_ID` bigint(20) DEFAULT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='issue流程过程中非系统字段的值';

-- ----------------------------
-- Table structure for T_ISSUE_FIELD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_FIELD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `LABEL` char(255) DEFAULT NULL,
  `NAME` char(255) DEFAULT NULL,
  `TYPE` char(255) NOT NULL,
  `TYPE_NAME` char(255) DEFAULT NULL,
  `CLASSIFY` char(2) DEFAULT NULL,
  `IS_SYS_FIELD` bit(1) DEFAULT NULL,
  `MARK` int(11) DEFAULT '0',
  `SORT` int(11) DEFAULT '5000',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_issue_field_name_index` (`NAME`,`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_FIELD_VALUE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_FIELD_VALUE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FIELD_ID` bigint(20) NOT NULL,
  `KEY` char(255) NOT NULL,
  `NAME` char(255) NOT NULL,
  `SORT` int(11) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_issue_field_value_project_id_key_index` (`KEY`,`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='存储下拉、单选、多选类似控件的预设值';

-- ----------------------------
-- Table structure for T_ISSUE_FOLLOW
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_FOLLOW` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='关注issue';

-- ----------------------------
-- Table structure for T_ISSUE_INSTANCE_FIELD_VALUE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_INSTANCE_FIELD_VALUE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `FIELD_ID` bigint(20) DEFAULT NULL,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `DISPLAY_VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `FIELD_ID` (`FIELD_ID`,`ISSUE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_LABEL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_LABEL` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `GROUP_ID` bigint(20) DEFAULT NULL,
  `NAME` char(255) DEFAULT NULL,
  `DESC` text,
  `COLOR` char(255) DEFAULT NULL,
  `STYLE` char(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_issue_label_group_id_index` (`GROUP_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_LABEL_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_LABEL_GROUP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  `IS_SYS_DEFAULT` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_LOG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_LOG` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `OPERATION` char(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='记录issue的操作日志';

-- ----------------------------
-- Table structure for T_ISSUE_RELATION_ISSUE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_RELATION_ISSUE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID_1` bigint(20) DEFAULT NULL,
  `ISSUE_ID_2` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='issue关联关系';

-- ----------------------------
-- Table structure for T_ISSUE_RELATION_LABEL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_RELATION_LABEL` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `LABEL_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_SELECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_SELECT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `CONDITION` text,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='筛选器';

-- ----------------------------
-- Table structure for T_ISSUE_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_TASK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_ID` bigint(20) DEFAULT NULL,
  `HANDLER_USER` varchar(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='记录issue当前处理人';

-- ----------------------------
-- Table structure for T_ISSUE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_TEMPLATE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `DESC` text,
  `DEFAULT_TEMPLATE` bit(1) DEFAULT b'0',
  `ENABLE` bit(1) DEFAULT b'1',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_TEMPLATE_FIELD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_TEMPLATE_FIELD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TEMPLATE_ID` bigint(20) DEFAULT NULL,
  `FIELD_ID` bigint(20) DEFAULT NULL,
  `SORT` int(11) DEFAULT NULL,
  `DEFAULT_VALUE` char(255) DEFAULT NULL,
  `REQUIRED` bit(1) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_issue_template_field_project_id_template_id_field_id_index` (`TEMPLATE_ID`,`FIELD_ID`,`PROJECT_ID`) USING BTREE,
  KEY `t_issue_template_field_template_id_index` (`TEMPLATE_ID`) USING BTREE,
  KEY `t_issue_template_field_field_id` (`FIELD_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_TYPE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_TYPE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TEMPLATE_ID` bigint(20) DEFAULT NULL,
  `TEMPLATE_NAME` varchar(255) DEFAULT NULL,
  `FLOW_ID` bigint(20) DEFAULT NULL,
  `FLOW_NAME` varchar(255) DEFAULT NULL,
  `NAME` char(255) DEFAULT NULL,
  `DESC` char(255) DEFAULT NULL,
  `CLASSIFY` char(255) DEFAULT NULL,
  `ICO` char(255) DEFAULT NULL,
  `IS_SYS_DEFAULT` bit(1) DEFAULT b'0',
  `ENABLE` bit(1) DEFAULT b'1',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='issue类型';

-- ----------------------------
-- Table structure for T_ISSUE_TYPE_FIELD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_TYPE_FIELD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ISSUE_TYPE_ID` bigint(20) DEFAULT NULL,
  `FIELD_ID` bigint(20) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `t_issue_type_field_issue_type_id_index` (`ISSUE_TYPE_ID`) USING BTREE,
  KEY `t_issue_type_field_field_id_index` (`FIELD_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_ISSUE_VERSION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ISSUE_VERSION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `STATE` char(255) DEFAULT 'UNPUBLISHED',
  `START_DATE` date DEFAULT NULL,
  `PLAN_RELEASE_DATE` date DEFAULT NULL,
  `ACTUAL_RELEASE_DATE` date DEFAULT NULL,
  `DESC` text,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_issue_version_name_index` (`PROJECT_ID`,`NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_CONFIG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_CONFIG` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ENABLE_MULTIPLE_BOARD` bit(1) DEFAULT b'1',
  `ENABLE_CODE_RELATION` bit(1) DEFAULT b'1',
  `KEYWORD` char(255) NOT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_project_config_keyword_index` (`KEYWORD`) USING BTREE,
  UNIQUE KEY `t_project_config_project_id_index` (`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_FILE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_FILE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ORIGINAL_NAME` char(255) DEFAULT NULL,
  `NEW_NAME` char(255) DEFAULT NULL,
  `PATH` char(255) DEFAULT NULL,
  `SIZE` bigint(20) DEFAULT NULL,
  `CONTEXT_TYPE` char(255) DEFAULT NULL,
  `PREVIEW` bit(1) DEFAULT b'0',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_file_new_name_index` (`NEW_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_LISTENER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_LISTENER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ERROR` text,
  `LISTENER` varchar(255) DEFAULT NULL,
  `JSON_SERIALIZABLE` text,
  `JDK_SERIALIZABLE` text,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_NOTICE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_NOTICE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MSG_TYPE` char(255) DEFAULT NULL,
  `WHO_TO_INFORM` char(255) DEFAULT '',
  `NOTICE_WAY` char(255) DEFAULT '',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_project_notice_project_id_create_user_index` (`PROJECT_ID`,`CREATED_USER`,`MSG_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_ROLE_RESOURCES
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_ROLE_RESOURCES` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ROLE_ID` varchar(255) DEFAULT NULL,
  `RESOURCES_ID` bigint(20) DEFAULT NULL,
  `TABLE_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_USER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_USER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` char(255) DEFAULT NULL,
  `VISIT_COUNT` int(11) DEFAULT NULL,
  `LAST_VISIT_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `DEPT` char(255) DEFAULT NULL,
  `STATE` char(255) DEFAULT NULL,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_PROJECT_USER_RESOURCES
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT_USER_RESOURCES` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` varchar(255) DEFAULT NULL,
  `RESOURCES_ID` bigint(20) DEFAULT NULL,
  `TABLE_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for T_SPRINT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_SPRINT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TITLE` varchar(255) DEFAULT NULL,
  `DESC` text,
  `PURPOSE` text,
  `START_TIME` date DEFAULT NULL,
  `END_TIME` date DEFAULT NULL,
  `STATE` varchar(64) DEFAULT NULL,
  `SORT` int(11) DEFAULT '0',
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  `UPDATED_USER` varchar(64) DEFAULT NULL,
  `UPDATED_TIME` datetime DEFAULT NULL,
  `IS_DELETE` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `t_sprint_title_index` (`TITLE`,`PROJECT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='迭代';

-- ----------------------------
-- View structure for T_ISSUE_VIEW
-- ----------------------------
DROP VIEW IF EXISTS `T_ISSUE_VIEW`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `T_ISSUE_VIEW` AS select `ti`.`ID` AS `ID`,`ti`.`PARENT_ID` AS `PARENT_ID`,`ti`.`NUMBER` AS `NUMBER`,`ti`.`TITLE` AS `TITLE`,`ti`.`ISSUE_TYPE_ID` AS `ISSUE_TYPE_ID`,`ti`.`PRIORITY` AS `PRIORITY`,`ti`.`STATE` AS `STATE`,`ti`.`FILE_ID` AS `FILE_ID`,`ti`.`DESC` AS `DESC`,`ti`.`SORT` AS `SORT`,`ti`.`ISSUE_TYPE_CLASSIFY` AS `ISSUE_TYPE_CLASSIFY`,`ti`.`COLOR` AS `COLOR`,`ti`.`PROJECT_ID` AS `PROJECT_ID`,`ti`.`CREATED_USER` AS `CREATED_USER`,`ti`.`CREATED_TIME` AS `CREATED_TIME`,`ti`.`UPDATED_USER` AS `UPDATED_USER`,`ti`.`UPDATED_TIME` AS `UPDATED_TIME`,`ti`.`IS_DELETE` AS `IS_DELETE`,`ti`.`CLOSE_REASON` AS `CLOSE_REASON`,`tit`.`HANDLER_USER` AS `PROCESSING`,`tid`.`CREATED_USER` AS `TO_HANDLE`,`tif`.`NAME` AS `FIELD_NAME`,`tif`.`LABEL` AS `FIELD_LABEL`,`tiifv`.`VALUE` AS `FIELD_VALUE`,`tiifv`.`DISPLAY_VALUE` AS `DISPLAY_VALUE`,`til`.`ID` AS `LABEL_ID`,`tfn`.`CATEGORY` AS `NODE_CLASSIFY` from (((((((`T_ISSUE` `ti` left join `T_ISSUE_INSTANCE_FIELD_VALUE` `tiifv` on((`ti`.`ID` = `tiifv`.`ISSUE_ID`))) left join `T_ISSUE_FIELD` `tif` on((`tiifv`.`FIELD_ID` = `tif`.`ID`))) left join `T_ISSUE_DIRECTION` `tid` on((`tid`.`ISSUE_ID` = `ti`.`ID`))) left join `T_ISSUE_TASK` `tit` on((`tit`.`ISSUE_ID` = `ti`.`ID`))) left join `T_ISSUE_RELATION_LABEL` `tirl` on((`ti`.`ID` = `tirl`.`ISSUE_ID`))) left join `T_ISSUE_LABEL` `til` on((`til`.`ID` = `tirl`.`LABEL_ID`))) left join `T_FLOW_NODE` `tfn` on((`tfn`.`ID` = `ti`.`STATE`)));

SET FOREIGN_KEY_CHECKS = 1;
