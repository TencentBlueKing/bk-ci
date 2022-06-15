USE devops_ci_artifactory;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_FILE_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FILE_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键ID',
  `PROJECT_CODE` varchar(64) DEFAULT '' COMMENT '用户组所属项目',
  `FILE_TYPE` varchar(32) NOT NULL DEFAULT '' COMMENT '文件类型',
  `FILE_PATH` varchar(1024) DEFAULT '' COMMENT '文件路径',
  `FILE_NAME` varchar(128) NOT NULL COMMENT '文件名字',
  `FILE_SIZE` bigint(20) NOT NULL COMMENT '文件大小',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `ID` varchar(32) NOT NULL COMMENT '主键ID',
  `PROPS_KEY` varchar(64) DEFAULT NULL COMMENT '属性字段key',
  `PROPS_VALUE` varchar(256) DEFAULT NULL COMMENT '属性字段value',
  `FILE_ID` varchar(32) NOT NULL COMMENT '文件ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tfpi_props_key` (`PROPS_KEY`),
  KEY `inx_tfpi_props_value` (`PROPS_VALUE`),
  KEY `inx_tfpi_file_id` (`FILE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据信息表';

-- ----------------------------
-- Table structure for T_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TOKEN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `ARTIFACTORY_TYPE` varchar(32) NOT NULL COMMENT '归档仓库类型',
  `PATH` text NOT NULL COMMENT '路径',
  `TOKEN` varchar(64) NOT NULL COMMENT 'TOKEN',
  `EXPIRE_TIME` datetime NOT NULL COMMENT '过期时间',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `TOKEN` (`TOKEN`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for T_FILE_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_FILE_TASK`  (
  `TASK_ID` varchar(64) NOT NULL COMMENT '任务ID',
  `FILE_TYPE` varchar(32)  NULL DEFAULT NULL COMMENT '文件类型',
  `FILE_PATH` text  NULL COMMENT '文件路径',
  `MACHINE_IP` varchar(32)  NULL DEFAULT NULL COMMENT '机器ip地址',
  `LOCAL_PATH` text  NULL COMMENT '本地路径',
  `STATUS` smallint(3) NULL DEFAULT NULL COMMENT '状态',
  `USER_ID` varchar(32)  NULL DEFAULT NULL COMMENT '用户ID',
  `PROJECT_ID` varchar(64)  NULL DEFAULT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(34)  NULL DEFAULT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(34)  NULL DEFAULT NULL COMMENT '构建ID',
  `CREATE_TIME` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime(0) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`TASK_ID`) USING BTREE,
  INDEX `idx_buildId`(`BUILD_ID`) USING BTREE,
  INDEX `idx_projectId_pipelineId`(`PROJECT_ID`, `PIPELINE_ID`) USING BTREE,
  INDEX `idx_updateTime`(`UPDATE_TIME`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COMMENT='文件托管任务表';