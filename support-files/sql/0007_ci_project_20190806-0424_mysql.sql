USE devops_ci_project;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_ACTIVITY
-- ----------------------------
DROP TABLE IF EXISTS `T_ACTIVITY`;
CREATE TABLE `T_ACTIVITY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(32) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `LINK` varchar(1024) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `STATUS` varchar(32) NOT NULL,
  `CREATOR` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_TYPE` (`NAME`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_MESSAGE_CODE_DETAIL
-- ----------------------------
DROP TABLE IF EXISTS `T_MESSAGE_CODE_DETAIL`;
CREATE TABLE `T_MESSAGE_CODE_DETAIL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `MESSAGE_CODE` varchar(128) NOT NULL COMMENT 'code码',
  `MODULE_CODE` char(2) NOT NULL COMMENT '模块代码',
  `MESSAGE_DETAIL_ZH_CN` varchar(500) NOT NULL COMMENT '中文简体描述信息',
  `MESSAGE_DETAIL_ZH_TW` varchar(500) DEFAULT NULL COMMENT '中文繁体描述信息',
  `MESSAGE_DETAIL_EN` varchar(500) DEFAULT NULL COMMENT '英文描述信息',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tmcd_message_code` (`MESSAGE_CODE`),
  KEY `inx_tmcd_module_code` (`MODULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='code码详情表';

-- ----------------------------
-- Table structure for T_USER
-- ----------------------------
DROP TABLE IF EXISTS `T_USER`;
CREATE TABLE `T_USER` (
  `USER_ID` varchar(64) NOT NULL,
  `NAME` varchar(64) NOT NULL,
  `BG_ID` int(11) NOT NULL,
  `BG_NAME` varchar(256) NOT NULL,
  `DEPT_ID` int(11) DEFAULT NULL,
  `DEPT_NAME` varchar(256) DEFAULT NULL,
  `CENTER_ID` int(11) DEFAULT NULL,
  `CENTER_NAME` varchar(256) DEFAULT NULL,
  `GROYP_ID` int(11) DEFAULT NULL,
  `GROUP_NAME` varchar(256) DEFAULT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_USER_DAILY_FIRST_AND_LAST_LOGIN
-- ----------------------------
DROP TABLE IF EXISTS `T_USER_DAILY_FIRST_AND_LAST_LOGIN`;
CREATE TABLE `T_USER_DAILY_FIRST_AND_LAST_LOGIN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` varchar(64) NOT NULL,
  `DATE` date NOT NULL,
  `FIRST_LOGIN_TIME` datetime NOT NULL,
  `LAST_LOGIN_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `DATE_AND_USER_ID` (`DATE`,`USER_ID`) USING BTREE,
  KEY `USER_ID_AND_DAE` (`USER_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_USER_DAILY_LOGIN
-- ----------------------------
DROP TABLE IF EXISTS `T_USER_DAILY_LOGIN`;
CREATE TABLE `T_USER_DAILY_LOGIN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` varchar(64) NOT NULL,
  `DATE` date NOT NULL,
  `LOGIN_TIME` datetime NOT NULL,
  `OS` varchar(32) NOT NULL,
  `IP` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `USER_ID_AND_DATE` (`USER_ID`,`DATE`) USING BTREE,
  KEY `DATE_AND_USER_ID` (`DATE`,`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_favorite
-- ----------------------------
DROP TABLE IF EXISTS `t_favorite`;
CREATE TABLE `t_favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `service_id` bigint(20) DEFAULT NULL COMMENT '服务id',
  `username` varchar(64) DEFAULT NULL COMMENT '用户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`service_id`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_project
-- ----------------------------
DROP TABLE IF EXISTS `t_project`;
CREATE TABLE `t_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `extra` text,
  `creator` varchar(32) DEFAULT NULL,
  `description` text,
  `kind` int(10) DEFAULT NULL,
  `cc_app_id` bigint(20) DEFAULT NULL,
  `cc_app_name` varchar(64) DEFAULT NULL,
  `is_offlined` bit(1) DEFAULT b'0',
  `project_id` varchar(32) NOT NULL,
  `project_name` varchar(64) NOT NULL,
  `english_name` varchar(64) NOT NULL,
  `updator` varchar(32) DEFAULT NULL,
  `project_type` int(10) DEFAULT NULL,
  `use_bk` bit(1) DEFAULT b'1',
  `deploy_type` text,
  `bg_id` bigint(20) DEFAULT NULL,
  `bg_name` varchar(255) DEFAULT NULL,
  `dept_id` bigint(20) DEFAULT NULL,
  `dept_name` varchar(255) DEFAULT NULL,
  `center_id` bigint(20) DEFAULT NULL,
  `center_name` varchar(255) DEFAULT NULL,
  `data_id` bigint(20) DEFAULT NULL,
  `is_secrecy` bit(1) DEFAULT b'0',
  `is_helm_chart_enabled` bit(1) DEFAULT b'0',
  `approval_status` int(10) DEFAULT '1',
  `logo_addr` text,
  `approver` varchar(32) DEFAULT NULL,
  `remark` text,
  `approval_time` timestamp NULL DEFAULT NULL,
  `creator_bg_name` varchar(128) DEFAULT '',
  `creator_dept_name` varchar(128) DEFAULT '',
  `creator_center_name` varchar(128) DEFAULT '',
  `hybrid_cc_app_id` bigint(20) DEFAULT NULL,
  `enable_external` bit(1) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `project_name` (`project_name`) USING BTREE,
  UNIQUE KEY `project_id` (`project_id`) USING BTREE,
  UNIQUE KEY `english_name` (`english_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_service
-- ----------------------------
DROP TABLE IF EXISTS `t_service`;
CREATE TABLE `t_service` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(64) DEFAULT NULL COMMENT '名称',
  `service_type_id` bigint(20) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `link_new` varchar(255) DEFAULT NULL,
  `inject_type` varchar(64) DEFAULT NULL,
  `iframe_url` varchar(4096) DEFAULT NULL,
  `css_url` varchar(4096) DEFAULT NULL,
  `js_url` varchar(4096) DEFAULT NULL,
  `show_project_list` bit(1) DEFAULT NULL,
  `show_nav` bit(1) DEFAULT NULL,
  `project_id_type` varchar(64) DEFAULT NULL,
  `status` varchar(64) DEFAULT NULL,
  `created_user` varchar(64) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` varchar(64) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT NULL,
  `gray_css_url` varchar(4096) DEFAULT NULL,
  `gray_js_url` varchar(4096) DEFAULT NULL,
  `weight` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_service_type
-- ----------------------------
DROP TABLE IF EXISTS `t_service_type`;
CREATE TABLE `t_service_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `title` varchar(64) DEFAULT NULL COMMENT '标题名称',
  `created_user` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_user` varchar(64) DEFAULT NULL COMMENT '修改人',
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bit(1) DEFAULT NULL COMMENT '删除标识符',
  `weight` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
