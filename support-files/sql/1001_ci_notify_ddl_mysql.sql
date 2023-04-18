USE devops_ci_notify;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_NOTIFY_EMAIL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_EMAIL` (
  `ID` varchar(32) NOT NULL COMMENT '主键ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '邮件来源',
  `SENDER` varchar(255) NOT NULL COMMENT '邮件发送者',
  `TO` text NOT NULL COMMENT '邮件接收者',
  `TITLE` varchar(255) NOT NULL COMMENT '邮件标题',
  `BODY` mediumtext NOT NULL COMMENT '邮件内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
  `CC` text COMMENT '邮件抄送接收者',
  `BCC` text COMMENT '邮件密送接收者',
  `FORMAT` int(11) NOT NULL COMMENT '格式',
  `TYPE` int(11) NOT NULL COMMENT '类型',
  `CONTENT_MD5` varchar(32) NOT NULL COMMENT '内容md5值，由title和body计算得，频率限制时使用',
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL COMMENT '频率限制时长，单位分钟，即n分钟内不重发成功的消息',
  `TOF_SYS_ID` varchar(20) DEFAULT NULL COMMENT 'tof系统id',
  `FROM_SYS_ID` varchar(20) DEFAULT NULL COMMENT '发送消息的系统id',
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`),
  KEY `idx_T_NOTIFY_EMAIL_CREATED_TIME` (`CREATED_TIME`) USING BTREE,
  KEY `idx_T_NOTIFY_EMAIL_CONTENT_MD5_SUCCESS_CREATED_TIME` (`CONTENT_MD5`,`SUCCESS`,`CREATED_TIME`) USING BTREE COMMENT '索引主要用于频率限制的查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_NOTIFY_RTX
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_RTX` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `BATCH_ID` varchar(32) NOT NULL COMMENT 'RTX通知批次ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '邮件来源',
  `SENDER` varchar(255) NOT NULL COMMENT '邮件发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `TITLE` varchar(255) NOT NULL COMMENT '邮件标题',
  `BODY` text NOT NULL COMMENT '邮件内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
  `CONTENT_MD5` varchar(32) NOT NULL COMMENT '内容md5值，由title和body计算得，频率限制时使用',
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL COMMENT '频率限制时长，单位分钟，即n分钟内不重发成功的消息',
  `TOF_SYS_id` varchar(20) DEFAULT NULL COMMENT 'tof系统id',
  `FROM_SYS_ID` varchar(20) DEFAULT NULL COMMENT '发送消息的系统id',
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='rtx流水表';

-- ----------------------------
-- Table structure for T_NOTIFY_SMS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_SMS` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '邮件来源',
  `SENDER` varchar(255) NOT NULL COMMENT '邮件发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `BODY` text NOT NULL COMMENT '邮件内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
  `BATCH_ID` varchar(32) NOT NULL COMMENT '通知批次ID',
  `T_NOTIFY_SMScol` varchar(45) DEFAULT NULL COMMENT '',
  `CONTENT_MD5` varchar(32) NOT NULL COMMENT '内容md5值，由title和body计算得，频率限制时使用',
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL COMMENT '频率限制时长，单位分钟，即n分钟内不重发成功的消息',
  `TOF_SYS_ID` varchar(20) DEFAULT NULL COMMENT 'tof系统id',
  `FROM_SYS_ID` varchar(20) DEFAULT NULL COMMENT '发送消息的系统id',
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_NOTIFY_WECHAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_WECHAT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `SUCCESS` bit(1) NOT NULL COMMENT '是否成功',
  `SOURCE` varchar(255) NOT NULL COMMENT '邮件来源',
  `SENDER` varchar(255) NOT NULL COMMENT '邮件发送者',
  `RECEIVERS` text NOT NULL COMMENT '通知接收者',
  `BODY` text NOT NULL COMMENT '邮件内容',
  `PRIORITY` int(11) NOT NULL COMMENT '优先级',
  `RETRY_COUNT` int(11) NOT NULL COMMENT '重试次数',
  `LAST_ERROR` text COMMENT '最后错误内容',
  `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
  `CONTENT_MD5` varchar(32) NOT NULL COMMENT '内容md5值，由title和body计算得，频率限制时使用',
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL COMMENT '频率限制时长，单位分钟，即n分钟内不重发成功的消息',
  `TOF_SYS_ID` varchar(20) DEFAULT NULL COMMENT 'tof系统id',
  `FROM_SYS_ID` varchar(20) DEFAULT NULL COMMENT '发送消息的系统id',
  `DelaySeconds` int(11) DEFAULT NULL COMMENT '延迟发送的时间，秒',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信流水表';

CREATE TABLE IF NOT EXISTS `T_COMMON_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                varchar(32)  NOT NULL COMMENT '主键ID',
    `TEMPLATE_CODE`     varchar(64)  NOT NULL COMMENT '模板代码',
    `TEMPLATE_NAME`     varchar(128) NOT NULL COMMENT '模板名称',
    `NOTIFY_TYPE_SCOPE` varchar(64)  NOT NULL COMMENT '适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）',
    `PRIORITY`          tinyint(4)   NOT NULL COMMENT '优先级',
    `SOURCE`            tinyint(4)   NOT NULL COMMENT '邮件来源',
    PRIMARY KEY (`ID`),
    KEY `idx_code` (`TEMPLATE_CODE`),
    KEY `idx_name` (`TEMPLATE_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='基础模板表';

 CREATE TABLE IF NOT EXISTS `T_WECHAT_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50) NOT NULL COMMENT '修改者',
    `SENDER`             varchar(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者',
    `TITLE`              varchar(256)         DEFAULT NULL COMMENT '邮件标题',
    `BODY`               mediumtext  NOT NULL COMMENT '邮件内容',
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='wechat模板表';

 CREATE TABLE IF NOT EXISTS `T_RTX_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50) NOT NULL COMMENT '修改者',
    `SENDER`             varchar(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者',
    `TITLE`              varchar(256)         DEFAULT NULL COMMENT '邮件标题',
    `BODY`               mediumtext  NOT NULL COMMENT '邮件内容',
	`BODY_MD` 			 mediumtext      NULL COMMENT 'markdown格式内容',
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='rtx模板表';

 CREATE TABLE IF NOT EXISTS `T_EMAILS_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50) NOT NULL COMMENT '修改者',
    `SENDER`             varchar(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者',
    `TITLE`              varchar(256)         DEFAULT NULL COMMENT '邮件标题',
    `BODY`               mediumtext  NOT NULL COMMENT '邮件内容',
    `BODY_FORMAT`        tinyint(4)  NOT NULL COMMENT '邮件格式（0:文本 1:html网页）',
    `EMAIL_TYPE`         tinyint(4)  NOT NULL COMMENT '邮件类型（0:外部邮件 1:内部邮件）',
    `TENCENT_CLOUD_TEMPLATE_ID`           int(11)     NULL     COMMENT '腾讯云邮件模板id',
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='email模板表';

-- ----------------------------
-- Table structure for T_NOTIFY_WEWORK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_WEWORK`
(
    `ID`           bigint primary key AUTO_INCREMENT COMMENT '主键ID',
    `SUCCESS`      bit(1) NOT NULL COMMENT '是否成功',
    `RECEIVERS`    text   NOT NULL COMMENT '通知接收者',
    `BODY`         text   NOT NULL COMMENT '邮件内容',
    `LAST_ERROR`   text COMMENT '最后错误内容',
    `CREATED_TIME` datetime(6) DEFAULT NOW(6) COMMENT '创建时间',
    `UPDATED_TIME` datetime(6) DEFAULT NOW(6) COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='企业微信流水表';

CREATE TABLE IF NOT EXISTS `T_WEWORK_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32)  NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32)  NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50)  NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50)  NOT NULL COMMENT '修改者',
    `SENDER`             varchar(128) NOT NULL DEFAULT 'DevOps' COMMENT '邮件发送者',
    `TITLE`              varchar(256)          DEFAULT NULL COMMENT '邮件标题',
    `BODY`               mediumtext   NOT NULL COMMENT '邮件内容',
    `CREATE_TIME`        datetime(6)     NOT NULL DEFAULT NOW(6) COMMENT '创建时间',
    `UPDATE_TIME`        datetime(6)     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='wework模板表';

CREATE TABLE IF NOT EXISTS `T_WEWORK_GROUP_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL COMMENT '主键ID',
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
    `CREATOR`            varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIOR`           varchar(50) NOT NULL COMMENT '修改者',
    `TITLE`              varchar(256)         DEFAULT NULL COMMENT '邮件标题',
    `BODY`               mediumtext  NOT NULL COMMENT '内容',
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='企业微信群模板表';

SET FOREIGN_KEY_CHECKS = 1;
