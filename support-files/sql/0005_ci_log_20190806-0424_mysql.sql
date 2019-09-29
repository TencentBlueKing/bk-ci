USE devops_ci_log;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_LOG_INDICES
-- ----------------------------
DROP TABLE IF EXISTS `T_LOG_INDICES`;
CREATE TABLE `T_LOG_INDICES` (
  `ID` varchar(64) NOT NULL,
  `INDEX_NAME` varchar(20) NOT NULL,
  `LAST_LINE_NUM` bigint(20) NOT NULL DEFAULT '1' COMMENT '最后行号',
  `CREATE_TYPE_MAPPING` bit(1) NOT NULL DEFAULT b'0' COMMENT 'create the type mapping or not',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
