USE devops_ci_artifactory;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_FILE_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FILE_INFO` (
  `ID` varchar(32) NOT NULL,
  `PROJECT_CODE` varchar(64) DEFAULT '',
  `FILE_TYPE` varchar(32) NOT NULL DEFAULT '',
  `FILE_PATH` varchar(1024) DEFAULT '',
  `FILE_NAME` varchar(128) NOT NULL,
  `FILE_SIZE` bigint(20) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tfi_project_code` (`PROJECT_CODE`),
  KEY `inx_tfi_file_type` (`FILE_TYPE`),
  KEY `inx_tfi_file_path` (`FILE_PATH`(128)),
  KEY `inx_tfi_file_name` (`FILE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- ----------------------------
-- Table structure for T_FILE_PROPS_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FILE_PROPS_INFO` (
  `ID` varchar(32) NOT NULL,
  `PROPS_KEY` varchar(64) DEFAULT NULL,
  `PROPS_VALUE` varchar(256) DEFAULT NULL,
  `FILE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tfpi_props_key` (`PROPS_KEY`),
  KEY `inx_tfpi_props_value` (`PROPS_VALUE`),
  KEY `inx_tfpi_file_id` (`FILE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据信息表';

-- ----------------------------
-- Table structure for T_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TOKEN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `USER_ID` varchar(64) NOT NULL,
  `PROJECT_ID` varchar(32) NOT NULL,
  `ARTIFACTORY_TYPE` varchar(32) NOT NULL,
  `PATH` text NOT NULL,
  `TOKEN` varchar(64) NOT NULL,
  `EXPIRE_TIME` datetime NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `TOKEN` (`TOKEN`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
