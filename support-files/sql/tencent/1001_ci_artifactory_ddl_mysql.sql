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

-- ----------------------------
-- Table structure for T_FILE_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FILE_TASK`  (
  `TASK_ID` varchar(64) NOT NULL,
  `FILE_TYPE` varchar(32)  NULL DEFAULT NULL,
  `FILE_PATH` text  NULL,
  `MACHINE_IP` varchar(32)  NULL DEFAULT NULL,
  `LOCAL_PATH` text  NULL,
  `STATUS` smallint(3) NULL DEFAULT NULL,
  `USER_ID` varchar(32)  NULL DEFAULT NULL,
  `PROJECT_ID` varchar(64)  NULL DEFAULT NULL,
  `PIPELINE_ID` varchar(34)  NULL DEFAULT NULL,
  `BUILD_ID` varchar(34)  NULL DEFAULT NULL,
  `CREATE_TIME` datetime(0) NULL DEFAULT NULL,
  `UPDATE_TIME` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`TASK_ID`) USING BTREE,
  INDEX `idx_buildId`(`BUILD_ID`) USING BTREE,
  INDEX `idx_projectId_pipelineId`(`PROJECT_ID`, `PIPELINE_ID`) USING BTREE,
  INDEX `idx_updateTime`(`UPDATE_TIME`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COMMENT='文件托管任务表';

-- ----------------------------
-- Table structure for T_SHORT_URL
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SHORT_URL` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `URL` mediumtext NOT NULL,
  `EXPIRED_TIME` datetime NOT NULL,
  `CREATED_USER` varchar(64) DEFAULT NULL,
  `CREATED_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=18638975 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_TIPELINE_ARTIFACETORY_INFO
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_TIPELINE_ARTIFACETORY_INFO` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PIPELINE_ID` varchar(34) NOT NULL DEFAULT '' COMMENT '流水线ID',
  `BUILD_ID` varchar(34) NOT NULL DEFAULT '' COMMENT '构建ID',
  `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
  `BUNDLE_ID` varchar(200) DEFAULT NULL,
  `BUILD_NUM` int(11) NOT NULL DEFAULT '0',
  `NAME` varchar(256) NOT NULL DEFAULT '' COMMENT '文件名',
  `FULL_NAME` varchar(200) NOT NULL DEFAULT '' COMMENT '文件全名',
  `PATH` varchar(200) NOT NULL DEFAULT '' COMMENT '文件路径',
  `FULL_PATH` varchar(200) NOT NULL DEFAULT '' COMMENT '文件全路径',
  `SIZE` int(10) NOT NULL DEFAULT '0' COMMENT '文件大小(byte)',
  `MODIFIED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `ARTIFACTORY_TYPE` varchar(20) NOT NULL DEFAULT '' COMMENT '仓库类型',
  `PROPERTIES` text NOT NULL COMMENT '元数据',
  `APP_VERSION` varchar(50) NOT NULL DEFAULT '' COMMENT 'app版本',
  `DATA_FROM` tinyint(2) DEFAULT '0' COMMENT '数据来源：0自然数据 1补偿数据',
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID` (`PIPELINE_ID`),
  KEY `MODIFIED_TIME` (`MODIFIED_TIME`),
  KEY `INX_TTAI_BUILD_ID` (`BUILD_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=51492129 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
