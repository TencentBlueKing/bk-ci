USE devops_ci_support;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APP_VERSION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_VERSION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `VERSION_ID` varchar(100) NOT NULL DEFAULT '' COMMENT '版本号',
  `RELEASE_DATE` datetime DEFAULT NULL COMMENT '发布日期',
  `RELEASE_CONTENT` text NOT NULL COMMENT '发布内容',
  `CHANNEL_TYPE` tinyint(1) NOT NULL DEFAULT '1' COMMENT '渠道类型（1:"安卓", 2:"IOS"）',
  `UPDATE_TYPE` int(11) NOT NULL DEFAULT '1' COMMENT '更新类型,1:强更 ,2:软更',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

-- ----------------------------
-- Table structure for T_MESSAGE_CODE_DETAIL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MESSAGE_CODE_DETAIL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `MESSAGE_CODE` varchar(128) NOT NULL COMMENT 'code码',
  `MODULE_CODE` char(2) NOT NULL COMMENT '模块代码',
  `MESSAGE_DETAIL_ZH_CN` varchar(500) NOT NULL COMMENT '中文简体描述信息',
  `MESSAGE_DETAIL_ZH_TW` varchar(500) DEFAULT NULL COMMENT '中文繁体描述信息',
  `MESSAGE_DETAIL_EN` varchar(500) DEFAULT NULL COMMENT '英文描述信息',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tmcd_message_code` (`MESSAGE_CODE`),
  KEY `inx_tmcd_module_code` (`MODULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='信息码详情表';

-- ----------------------------
-- Table structure for T_NOTICE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTICE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NOTICE_TITLE` varchar(100) NOT NULL,
  `EFFECT_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `INVALID_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `CREATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `NOTICE_CONTENT` text NOT NULL,
  `REDIRECT_URL` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `inx_tn_effect_date` (`EFFECT_DATE`),
  KEY `inx_tn_invalid_date` (`INVALID_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_WECHAT_WORK_MESSAGE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_WECHAT_WORK_MESSAGE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MESSAGE_ID` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `MESSAGE_ID_INDEX` (`MESSAGE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_WECHAT_WORK_PROJECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_WECHAT_WORK_PROJECT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `GROUP_ID` varchar(32) NOT NULL DEFAULT '',
  `PROJECT_ID` varchar(32) NOT NULL DEFAULT '',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;


SET FOREIGN_KEY_CHECKS = 1;
