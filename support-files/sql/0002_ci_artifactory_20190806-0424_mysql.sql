USE devops_ci_artifactory;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_FILE_INFO
-- ----------------------------
DROP TABLE IF EXISTS `T_FILE_INFO`;
CREATE TABLE `T_FILE_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `PROJECT_CODE` varchar(64) DEFAULT '' COMMENT '项目代码',
  `FILE_TYPE` varchar(32) NOT NULL DEFAULT '' COMMENT '文件类型 bk-archive：流水线方式文件归档，bk-custom：自定义路径方式文件归档，bk-report：报告产出物归档',
  `FILE_PATH` varchar(1024) DEFAULT '' COMMENT '文件路径',
  `FILE_NAME` varchar(128) NOT NULL COMMENT '文件名称',
  `FILE_SIZE` bigint(20) NOT NULL COMMENT '文件大小（单位：字节）',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tfi_project_code` (`PROJECT_CODE`),
  KEY `inx_tfi_file_type` (`FILE_TYPE`),
  KEY `inx_tfi_file_path` (`FILE_PATH`),
  KEY `inx_tfi_file_name` (`FILE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='文件信息表';

-- ----------------------------
-- Table structure for T_FILE_PROPS_INFO
-- ----------------------------
DROP TABLE IF EXISTS `T_FILE_PROPS_INFO`;
CREATE TABLE `T_FILE_PROPS_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `PROPS_KEY` varchar(64) DEFAULT NULL COMMENT '元数据KEY值',
  `PROPS_VALUE` varchar(256) DEFAULT NULL COMMENT '元数据VALUE值',
  `FILE_ID` varchar(32) NOT NULL COMMENT '文件ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tfpi_props_key` (`PROPS_KEY`),
  KEY `inx_tfpi_props_value` (`PROPS_VALUE`),
  KEY `inx_tfpi_file_id` (`FILE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='文件元数据信息表';

SET FOREIGN_KEY_CHECKS = 1;
