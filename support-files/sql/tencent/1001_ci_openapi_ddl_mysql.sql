USE devops_ci_openapi;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APP_CODE_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_APP_CODE_GROUP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_CODE` varchar(255) NOT NULL,
  `BG_ID` int(11) DEFAULT NULL,
  `BG_NAME` varchar(255) DEFAULT NULL,
  `DEPT_ID` int(11) DEFAULT NULL,
  `DEPT_NAME` varchar(255) DEFAULT NULL,
  `CENTER_ID` int(11) DEFAULT NULL,
  `CENTER_NAME` varchar(255) DEFAULT NULL,
  `CREATOR` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATER` varchar(255) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_APP_CODE_PROJECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_APP_CODE_PROJECT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_CODE` varchar(255) NOT NULL,
  `PROJECT_ID` varchar(255) NOT NULL,
  `CREATOR` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


SET FOREIGN_KEY_CHECKS = 1;

