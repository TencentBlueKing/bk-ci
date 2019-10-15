USE devops_ci_log;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_LOG_INDICES
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LOG_INDICES` (
  `ID` varchar(64) NOT NULL,
  `INDEX_NAME` varchar(20) NOT NULL,
  `LAST_LINE_NUM` bigint(20) NOT NULL DEFAULT '1',
  `CREATE_TYPE_MAPPING` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
