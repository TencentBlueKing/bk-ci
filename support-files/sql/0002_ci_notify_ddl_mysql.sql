USE devops_ci_notify;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_NOTIFY_EMAIL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_EMAIL` (
  `ID` varchar(32) NOT NULL,
  `SUCCESS` bit(1) NOT NULL,
  `SOURCE` varchar(255) NOT NULL,
  `SENDER` varchar(255) NOT NULL,
  `TO` text NOT NULL,
  `TITLE` varchar(255) NOT NULL,
  `BODY` text NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `RETRY_COUNT` int(11) NOT NULL,
  `LAST_ERROR` text,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `CC` text,
  `BCC` text,
  `FORMAT` int(11) NOT NULL,
  `TYPE` int(11) NOT NULL,
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `idx_T_NOTIFY_EMAIL_CREATED_TIME` (`CREATED_TIME`) USING BTREE,
  KEY `idx_T_NOTIFY_EMAIL_CONTENT_MD5_SUCCESS_CREATED_TIME` (`CONTENT_MD5`,`SUCCESS`,`CREATED_TIME`) USING BTREE COMMENT '索引主要用于频率限制的查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_NOTIFY_RTX
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_RTX` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `BATCH_ID` varchar(32) NOT NULL,
  `SUCCESS` bit(1) NOT NULL,
  `SOURCE` varchar(255) NOT NULL,
  `SENDER` varchar(255) NOT NULL,
  `RECEIVERS` text NOT NULL,
  `TITLE` varchar(255) NOT NULL,
  `BODY` text NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `RETRY_COUNT` int(11) NOT NULL,
  `LAST_ERROR` text,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_id` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_NOTIFY_SMS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_SMS` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `SUCCESS` bit(1) NOT NULL,
  `SOURCE` varchar(255) NOT NULL,
  `SENDER` varchar(255) NOT NULL,
  `RECEIVERS` text NOT NULL,
  `BODY` text NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `RETRY_COUNT` int(11) NOT NULL,
  `LAST_ERROR` text,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `BATCH_ID` varchar(32) NOT NULL,
  `T_NOTIFY_SMScol` varchar(45) DEFAULT NULL,
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_NOTIFY_WECHAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_NOTIFY_WECHAT` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `SUCCESS` bit(1) NOT NULL,
  `SOURCE` varchar(255) NOT NULL,
  `SENDER` varchar(255) NOT NULL,
  `RECEIVERS` text NOT NULL,
  `BODY` text NOT NULL,
  `PRIORITY` int(11) NOT NULL,
  `RETRY_COUNT` int(11) NOT NULL,
  `LAST_ERROR` text,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `CONTENT_MD5` varchar(32) NOT NULL,
  `FREQUENCY_LIMIT` int(11) DEFAULT NULL,
  `TOF_SYS_ID` varchar(20) DEFAULT NULL,
  `FROM_SYS_ID` varchar(20) DEFAULT NULL,
  `DelaySeconds` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_COMMON_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                varchar(32)  NOT NULL,
    `TEMPLATE_CODE`     varchar(64)  NOT NULL,
    `TEMPLATE_NAME`     varchar(128) NOT NULL,
    `NOTIFY_TYPE_SCOPE` varchar(64)  NOT NULL,
    `PRIORITY`          tinyint(4)   NOT NULL,
    `SOURCE`            tinyint(4)   NOT NULL,
    PRIMARY KEY (`ID`),
    KEY `idx_code` (`TEMPLATE_CODE`),
    KEY `idx_name` (`TEMPLATE_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

 CREATE TABLE IF NOT EXISTS `T_WECHAT_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL,
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
    `CREATOR`            varchar(50) NOT NULL,
    `MODIFIOR`           varchar(50) NOT NULL,
    `TITLE`              varchar(256)         DEFAULT NULL,
    `BODY`               mediumtext  NOT NULL,
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

 CREATE TABLE IF NOT EXISTS `T_RTX_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL,
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
    `CREATOR`            varchar(50) NOT NULL,
    `MODIFIOR`           varchar(50) NOT NULL,
    `TITLE`              varchar(256)         DEFAULT NULL,
    `BODY`               mediumtext  NOT NULL,
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

 CREATE TABLE IF NOT EXISTS `T_EMAILS_NOTIFY_MESSAGE_TEMPLATE`
(
    `ID`                 varchar(32) NOT NULL,
    `COMMON_TEMPLATE_ID` varchar(32) NOT NULL,
    `CREATOR`            varchar(50) NOT NULL,
    `MODIFIOR`           varchar(50) NOT NULL,
    `TITLE`              varchar(256)         DEFAULT NULL,
    `BODY`               mediumtext  NOT NULL,
    `BODY_FORMAT`        tinyint(4)  NOT NULL,
    `EMAIL_TYPE`         tinyint(4)  NOT NULL,
    `CREATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `idx_templateId` (`COMMON_TEMPLATE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
