USE devops_ci_prebuild;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_PREBUILD_PERSONAL_MACHINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_PREBUILD_PERSONAL_MACHINE` (
  `OWNER` varchar(64) NOT NULL COMMENT '用户ID',
  `HOST_NAME` varchar(64) NOT NULL COMMENT '主机名',
  `IP` varchar(64) NOT NULL COMMENT 'IP',
  `REMARK` varchar(64) DEFAULT NULL COMMENT '备注',
  `CREATED_TIME` timestamp NULL DEFAULT NULL,
  `UPDATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`OWNER`,`HOST_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='个人开发机信息';

-- ----------------------------
-- Table structure for T_PREBUILD_PERSONAL_VM
-- ----------------------------

CREATE TABLE IF NOT EXISTS  `T_PREBUILD_PERSONAL_VM` (
  `OWNER` varchar(64) NOT NULL COMMENT '用户ID',
  `VM_IP` varchar(64) NOT NULL COMMENT 'IP',
  `VM_NAME` varchar(64) DEFAULT NULL COMMENT '名称',
  `RSYNC_PWD` varchar(64) DEFAULT NULL COMMENT 'rsync密码',
  `CREATED_TIME` timestamp NULL DEFAULT NULL,
  `UPDATE_TIME` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`OWNER`,`VM_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='个人开发机信息';

-- ----------------------------
-- Table structure for T_PREBUILD_PLUGIN_VERSION
-- ----------------------------
CREATE TABLE IF NOT EXISTS  `T_PREBUILD_PLUGIN_VERSION` (
  `VERSION` varchar(64) NOT NULL COMMENT '版本',
  `UPDATE_TIME` datetime NOT NULL COMMENT '修改时间',
  `MODIFY_USER` varchar(64) NOT NULL COMMENT '修改人',
  `DESC` varchar(255) NOT NULL COMMENT '描述',
  `PLUGIN_TYPE` varchar(64) NOT NULL COMMENT '插件的类型jetbrains,vscode....',
  PRIMARY KEY (`VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PREBUILD_PROJECT
-- ----------------------------
CREATE TABLE IF NOT EXISTS  `T_PREBUILD_PROJECT` (
  `PREBUILD_PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT 'prebuild项目ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '蓝盾项目ID',
  `OWNER` varchar(64) NOT NULL COMMENT '用户ID',
  `DESC` varchar(255) DEFAULT NULL COMMENT '描述',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `CREATOR` varchar(64) NOT NULL COMMENT '创建人',
  `UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `LAST_MODIFY_USER` varchar(64) NOT NULL COMMENT '最后更新人',
  `YAML` longtext NOT NULL,
  `PIPELINE_ID` varchar(64) DEFAULT NULL COMMENT '流水线ID',
  `WORKSPACE` varchar(4096) DEFAULT NULL,
  `SECRET_KEY` varchar(256) DEFAULT NULL,
  `IDE_VERSION` varchar(255) DEFAULT NULL COMMENT '用户的IDE的版本',
  `PLUGIN_VERSION` varchar(255) DEFAULT NULL COMMENT '用户使用的插件版本',
  PRIMARY KEY (`PREBUILD_PROJECT_ID`,`OWNER`),
  KEY `PROJECT_ID` (`PROJECT_ID`,`PREBUILD_PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='prebuild项目定义表';

-- ----------------------------
-- Table structure for T_WEBIDE_IDEINFO
-- ----------------------------
CREATE TABLE IF NOT EXISTS  `T_WEBIDE_IDEINFO` (
  `OWNER` varchar(64) COLLATE utf8_bin NOT NULL,
  `IP` varchar(64) COLLATE utf8_bin NOT NULL,
  `SERVER_TYPE` varchar(64) COLLATE utf8_bin NOT NULL,
  `AGENT_STATUS` int(11) DEFAULT '0',
  `IDE_STATUS` int(11) DEFAULT '0',
  `IDE_VERSION` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `SERVER_CREATE_TIME` bigint(11) DEFAULT '0',
  `PIPELINE_ID` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `IDE_LAST_UPDATE` bigint(11) DEFAULT '0',
  `DISK_GB` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `CPU_CORE` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `MEMORY_GB` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `SERVER_REGION_NAME` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`OWNER`,`IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='IDE实例状态表';

-- ----------------------------
-- Table structure for T_WEBIDE_OPENDIR
-- ----------------------------
CREATE TABLE IF NOT EXISTS  `T_WEBIDE_OPENDIR` (
  `OWNER` varchar(64) COLLATE utf8_bin NOT NULL,
  `IP` varchar(45) COLLATE utf8_bin NOT NULL,
  `PATH` varchar(45) COLLATE utf8_bin NOT NULL,
  `LAST_UPDATE` bigint(11) DEFAULT NULL,
  PRIMARY KEY (`OWNER`,`IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='records of open path ';

SET FOREIGN_KEY_CHECKS = 1;
