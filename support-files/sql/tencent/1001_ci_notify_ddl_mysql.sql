USE devops_ci_notify;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_BASE_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_BASE_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL COMMENT 'ä¸»é”®',
  `TEMPLATE_CODE` varchar(64) NOT NULL COMMENT 'æ¨¡æ¿ä»£ç ',
  `TEMPLATE_NAME` varchar(128) NOT NULL COMMENT 'æ¨¡æ¿åç§°',
  `NOTIFY_TYPE_SCOPE` varchar(64) NOT NULL COMMENT 'é€‚ç”¨çš„é€šçŸ¥ç±»åž‹ï¼ˆEMAIL:é‚®ä»¶ RTX:ä¼ä¸šå¾®ä¿¡ WECHAT:å¾®ä¿¡ SMS:çŸ­ä¿¡ï¼‰',
  `TITLE` varchar(256) DEFAULT '' COMMENT 'æ ‡é¢˜',
  `BODY` mediumtext NOT NULL COMMENT 'å†…å®¹',
  `PRIORITY` tinyint(4) NOT NULL DEFAULT '-1' COMMENT 'ä¼˜å…ˆçº§åˆ«ï¼ˆ-1:ä½Ž 0:æ™®é€š 1:é«˜ï¼‰',
  `SOURCE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€šçŸ¥æ¥æºï¼ˆ0:æœ¬åœ°ä¸šåŠ¡ 1:æ“ä½œï¼‰',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT 'åˆ›å»ºäºº',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT 'æœ€è¿‘ä¿®æ”¹äºº',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_tbnmt_template_code` (`TEMPLATE_CODE`),
  UNIQUE KEY `uni_tbnmt_template_name` (`TEMPLATE_NAME`),
  KEY `inx_tbnmt_type_scope` (`NOTIFY_TYPE_SCOPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_COMMON_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_COMMON_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL,
  `TEMPLATE_CODE` varchar(64) NOT NULL,
  `TEMPLATE_NAME` varchar(128) NOT NULL,
  `NOTIFY_TYPE_SCOPE` varchar(64) NOT NULL,
  `PRIORITY` tinyint(4) NOT NULL,
  `SOURCE` tinyint(4) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `idx_code` (`TEMPLATE_CODE`),
  KEY `idx_name` (`TEMPLATE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_EMAIL_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_EMAIL_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL COMMENT 'ä¸»é”®',
  `BASE_TEMPLATE_ID` varchar(32) NOT NULL COMMENT 'æ¨¡æ¿IDï¼ˆå¯¹åº”T_BASE_NOTIFY_MESSAGE_TEMPLATEè¡¨çš„ä¸»é”®ï¼‰',
  `BODY_FORMAT` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é‚®ä»¶æ ¼å¼ï¼ˆ0:æ–‡æœ¬ 1:htmlç½‘é¡µï¼‰',
  `EMAIL_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é‚®ä»¶ç±»åž‹ï¼ˆ0:å¤–éƒ¨é‚®ä»¶ 1:å†…éƒ¨é‚®ä»¶ï¼‰',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT 'åˆ›å»ºäºº',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT 'æœ€è¿‘ä¿®æ”¹äºº',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_tenmt_template_id` (`BASE_TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ;

-- ----------------------------
-- Table structure for T_EMAILS_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_EMAILS_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL,
  `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL,
  `MODIFIOR` varchar(50) NOT NULL,
  `TITLE` varchar(256) DEFAULT NULL,
  `BODY` mediumtext NOT NULL,
  `BODY_FORMAT` tinyint(4) NOT NULL,
  `EMAIL_TYPE` tinyint(4) NOT NULL,
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SENDER` varchar(128) NOT NULL DEFAULT 'DevOps',
  PRIMARY KEY (`ID`),
  KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_NOTIFY_EMAIL
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_NOTIFY_EMAIL` (
  `ID` varchar(32) NOT NULL COMMENT '邮件通知ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '邮件来源',
  `SENDER` varchar(255) NOT NULL COMMENT '邮件发送者',
  `TO` text NOT NULL /*!99104 COMPRESSED */ COMMENT '邮件接收者',
  `TITLE` varchar(255) NOT NULL COMMENT '邮件标题',
  `BODY` mediumtext NOT NULL /*!99104 COMPRESSED */ COMMENT '邮件内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text /*!99104 COMPRESSED */ COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '记录创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '记录更新时间',
  `CC` text /*!99104 COMPRESSED */ COMMENT '邮件抄送接收者',
  `BCC` text /*!99104 COMPRESSED */ COMMENT '邮件密送接收者',
  `FORMAT` int(11) NOT NULL COMMENT '邮件格式',
  `TYPE` int(11) NOT NULL COMMENT '邮件类型',
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL COMMENT '发送消息的系统id',
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`),
  KEY `idx_T_NOTIFY_EMAIL_CREATED_TIME` (`CREATED_TIME`),
  KEY `idx_T_NOTIFY_EMAIL_CONTENT_MD5_SUCCESS_CREATED_TIME` (`CONTENT_MD5`,`SUCCESS`,`CREATED_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_NOTIFY_RTX
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_NOTIFY_RTX` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT 'RTX通知ID',
  `BATCH_ID` varchar(32) NOT NULL COMMENT 'RTX通知批次ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '通知来源',
  `SENDER` varchar(255) NOT NULL COMMENT '通知发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `TITLE` varchar(255) NOT NULL COMMENT 'RTX通知标题',
  `BODY` text NOT NULL COMMENT 'RTX通知内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '记录创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '记录最后更新时间',
  `CONTENT_MD5` varchar(32) NOT NULL COMMENT '内容md5值，由title和body计算得，频率限制时使用',
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL COMMENT '频率限制时长，单位分钟，即n分钟内不重发成功的消息',
  `TOF_SYS_id` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_NOTIFY_SMS
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_NOTIFY_SMS` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '短信通知ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '通知来源',
  `SENDER` varchar(255) NOT NULL COMMENT '通知发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `BODY` text NOT NULL COMMENT '通知内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '记录创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '记录更新时间',
  `BATCH_ID` varchar(32) NOT NULL COMMENT '通知批次ID',
  `T_NOTIFY_SMScol` varchar(45) DEFAULT NULL,
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_NOTIFY_WECHAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_NOTIFY_WECHAT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '微信通知ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '通知来源',
  `SENDER` varchar(255) NOT NULL COMMENT '通知发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `BODY` text NOT NULL COMMENT '通知内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '记录创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '记录更新时间',
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_RTX_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_RTX_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL,
  `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL,
  `MODIFIOR` varchar(50) NOT NULL,
  `TITLE` varchar(256) DEFAULT NULL,
  `BODY` mediumtext NOT NULL,
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SENDER` varchar(128) NOT NULL DEFAULT 'DevOps',
  PRIMARY KEY (`ID`),
  KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_WECHAT_NOTIFY_MESSAGE_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_WECHAT_NOTIFY_MESSAGE_TEMPLATE` (
  `ID` varchar(32) NOT NULL,
  `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL,
  `MODIFIOR` varchar(50) NOT NULL,
  `TITLE` varchar(256) DEFAULT NULL,
  `BODY` mediumtext NOT NULL,
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SENDER` varchar(128) NOT NULL DEFAULT 'IEG_blueking',
  PRIMARY KEY (`ID`),
  KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `T_MOA_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32)  NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32)  NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50)  NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50)  NOT NULL COMMENT '修改者',
    `TITLE`              varchar(256)          DEFAULT NULL COMMENT '标题',
	`CALLBACK_URL`		 varchar(256)          DEFAULT NULL COMMENT '回调地址',
	`PROCESS_NAME`       varchar(256) NOT NULL COMMENT '流程名称: 单据所属的业务流程名称，由业务传入',
    `BODY`               mediumtext   NOT NULL COMMENT '内容',
    `CREATE_TIME`        datetime(6)     NOT NULL DEFAULT NOW(6) COMMENT '创建时间',
    `UPDATE_TIME`        datetime(6)     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='MOA模板表';

SET FOREIGN_KEY_CHECKS = 1;

