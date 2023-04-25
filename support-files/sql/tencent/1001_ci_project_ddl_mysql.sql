USE devops_ci_project;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_ACTIVITY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ACTIVITY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(32) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `ENGLISH_NAME` varchar(128) DEFAULT NULL,
  `LINK` varchar(1024) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `STATUS` varchar(32) NOT NULL,
  `CREATOR` varchar(32) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_TYPE` (`NAME`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_MESSAGE_CODE_DETAIL
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_MESSAGE_CODE_DETAIL` (
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
CREATE TABLE IF NOT EXISTS `T_USER` (
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
CREATE TABLE IF NOT EXISTS `T_USER_DAILY_FIRST_AND_LAST_LOGIN` (
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
CREATE TABLE IF NOT EXISTS `T_USER_DAILY_LOGIN` (
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
-- Table structure for T_FAVORITE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_FAVORITE` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `service_id` bigint(20) DEFAULT NULL COMMENT '服务id',
  `username` varchar(64) DEFAULT NULL COMMENT '用户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`service_id`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PROJECTS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT` (
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
  `project_name` varchar(64) NOT NULL COLLATE utf8mb4_bin,
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
  `enable_idc` bit(1) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `CHANNEL` varchar(32) NOT NULL DEFAULT 'BS',
  `pipeline_limit` int(10) DEFAULT 500 COMMENT '流水线数量上限',
  `other_router_tags` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `project_name` (`project_name`) USING BTREE,
  UNIQUE KEY `project_id` (`project_id`) USING BTREE,
  UNIQUE KEY `english_name` (`english_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_SERVICE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SERVICE` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(64) DEFAULT NULL COMMENT '名称',
  `english_name` varchar(64) DEFAULT NULL COMMENT '英文名称',
  `service_type_id` bigint(20) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `link_new` varchar(255) DEFAULT NULL,
  `inject_type` varchar(64) DEFAULT NULL,
  `iframe_url` varchar(255) DEFAULT NULL,
  `css_url` varchar(255) DEFAULT NULL,
  `js_url` varchar(255) DEFAULT NULL,
  `show_project_list` bit(1) DEFAULT NULL,
  `show_nav` bit(1) DEFAULT NULL,
  `project_id_type` varchar(64) DEFAULT NULL,
  `status` varchar(64) DEFAULT NULL,
  `created_user` varchar(64) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` varchar(64) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT NULL,
  `gray_css_url` varchar(255) DEFAULT NULL,
  `gray_js_url` varchar(255) DEFAULT NULL,
  `logo_url` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `web_socket` text COMMENT '支持webSocket的页面',
  `weight` int(11) DEFAULT NULL,
  `gray_iframe_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_SERVICE_ITEM
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SERVICE_ITEM` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ITEM_CODE` varchar(64) NOT NULL COMMENT '服务功能项代码',
  `ITEM_NAME` varchar(64) NOT NULL COMMENT '服务功能项名称',
  `HTML_PATH` varchar(1024) NOT NULL DEFAULT '' COMMENT '服务功能项对应的前端页面路径',
  `HTML_COMPONENT_TYPE` varchar(64) NOT NULL DEFAULT '' COMMENT '服务功能项对应的前端组件类型',
  `ITEM_STATUS` varchar(32) NOT NULL DEFAULT 'ENABLE' COMMENT '服务功能项状态 ENABLE:启用 DISABLE:禁用 DELETE:删除',
  `ENTRY_RES_URL` varchar(1024) NOT NULL DEFAULT '' COMMENT '前端入口资源路径',
  `ICON_URL` varchar(256) DEFAULT NULL COMMENT 'icon地址',
  `TOOLTIP` varchar(1024) DEFAULT NULL COMMENT '提示信息',
  `PROPS` text COMMENT '自定义扩展点前端表单属性配置Json串',
  `SERVICE_NUM` int(11) DEFAULT '0' COMMENT '扩展服务数量',
  `PARENT_ID` varchar(32) NOT NULL COMMENT '父服务功能项ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tsi_item_code` (`ITEM_CODE`),
  KEY `inx_tsi_parent_id` (`PARENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='服务功能项信息表';

-- ----------------------------
-- Table structure for T_SERVICE_TYPE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_SERVICE_TYPE` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(64) DEFAULT NULL,
  `english_title` varchar(64) DEFAULT NULL,
  `created_user` varchar(64) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_user` varchar(64) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT NULL,
  `weight` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_GRAY_TEST`
(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `service_id` bigint(20) DEFAULT NULL COMMENT '服务id',
  `username` varchar(64) DEFAULT NULL COMMENT '用户',
  `status` varchar(64) DEFAULT NULL COMMENT '服务状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`service_id`,`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_MEASURE_BUILD_ELEMENT` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `atomCode` varchar(255) DEFAULT NULL,
  `begintime` datetime NOT NULL,
  `buildid` varchar(34) NOT NULL,
  `elementid` varchar(64) NOT NULL,
  `elementname` varchar(64) NOT NULL,
  `endtime` datetime NOT NULL,
  `extra` varchar(100) DEFAULT NULL,
  `pipelineid` varchar(34) NOT NULL,
  `projectid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '',
    `LABEL_NAME`  varchar(45) NOT NULL,
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_inx_tmpl_name` (`LABEL_NAME`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '',
    `LABEL_ID`    varchar(32) NOT NULL,
    `PROJECT_ID`  varchar(32) NOT NULL,
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_tmplr_label_id` (`LABEL_ID`),
    KEY `inx_tmplr_project_id` (`PROJECT_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_NOTICE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NOTICE_TITLE` varchar(100) NOT NULL COMMENT '公告标题',
  `EFFECT_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '生效日期',
  `INVALID_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失效日期',
  `CREATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  `UPDATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新日期',
  `NOTICE_CONTENT` text NOT NULL COMMENT '公告内容',
  `REDIRECT_URL` varchar(200) DEFAULT NULL COMMENT '跳转地址',
  `NOTICE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息类型:0.弹框 1.跑马灯',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tn_effect_date` (`EFFECT_DATE`) USING BTREE,
  KEY `inx_tn_invalid_date` (`INVALID_DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
