USE devops_ci_monitoring;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_ALERT_USER
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_ALERT_USER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `SERVICE` varchar(32) NOT NULL,
  `LEVEL` varchar(32) NOT NULL,
  `USERS` varchar(256) DEFAULT NULL,
  `NOTIFY_TYPES` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `SERVICE_LEVEL` (`SERVICE`,`LEVEL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_SLA_DAILY
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_SLA_DAILY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `MODULE` varchar(20) NOT NULL COMMENT '模块',
  `NAME` varchar(20) NOT NULL COMMENT '名称',
  `SUCCESS_PERCENT` double(6,2) NOT NULL COMMENT '成功率',
  `START_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `END_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
  PRIMARY KEY (`ID`),
  KEY `idx_module_name` (`MODULE`,`NAME`),
  KEY `idx_start_time` (`START_TIME`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';


SET FOREIGN_KEY_CHECKS = 1;

