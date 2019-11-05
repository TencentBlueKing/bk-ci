USE devops_ci_store;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APPS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APPS` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(64) NOT NULL DEFAULT '',
  `OS` varchar(32) NOT NULL DEFAULT '',
  `BIN_PATH` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_OS` (`NAME`,`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境信息表';

-- ----------------------------
-- Table structure for T_APP_ENV
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_ENV` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `APP_ID` int(11) NOT NULL,
  `PATH` varchar(32) NOT NULL DEFAULT '',
  `NAME` varchar(32) NOT NULL DEFAULT '',
  `DESCRIPTION` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境变量表';

-- ----------------------------
-- Table structure for T_APP_VERSION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_VERSION` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `APP_ID` int(11) NOT NULL,
  `VERSION` varchar(32) DEFAULT '',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `APP_ID` (`APP_ID`,`VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境版本信息表';

-- ----------------------------
-- Table structure for T_ATOM
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `NAME` varchar(64) NOT NULL,
  `ATOM_CODE` varchar(64) NOT NULL,
  `CLASS_TYPE` varchar(64) NOT NULL,
  `SERVICE_SCOPE` varchar(256) NOT NULL,
  `JOB_TYPE` varchar(20) DEFAULT NULL,
  `OS` varchar(100) NOT NULL,
  `CLASSIFY_ID` varchar(32) NOT NULL,
  `DOCS_LINK` varchar(256) DEFAULT NULL,
  `ATOM_TYPE` tinyint(4) NOT NULL DEFAULT '1',
  `ATOM_STATUS` tinyint(4) NOT NULL,
  `ATOM_STATUS_MSG` varchar(1024) DEFAULT NULL,
  `SUMMARY` varchar(256) DEFAULT NULL,
  `DESCRIPTION` text,
  `CATEGROY` tinyint(4) NOT NULL DEFAULT '1',
  `VERSION` varchar(20) NOT NULL,
  `LOGO_URL` varchar(256) DEFAULT NULL,
  `ICON` text,
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0',
  `LATEST_FLAG` bit(1) NOT NULL,
  `BUILD_LESS_RUN_FLAG` bit(1) DEFAULT NULL,
  `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL,
  `CODE_SRC` varchar(256) DEFAULT NULL,
  `PAY_FLAG` bit(1) DEFAULT b'1',
  `HTML_TEMPLATE_VERSION` varchar(10) NOT NULL DEFAULT '1.1',
  `PROPS` text,
  `DATA` text,
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system',
  `WEIGHT` int(11) DEFAULT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpca_code_version` (`ATOM_CODE`,`VERSION`),
  KEY `inx_tpca_service_code` (`SERVICE_SCOPE`(255)),
  KEY `inx_tpca_os` (`OS`),
  KEY `inx_tpca_atom_code` (`ATOM_CODE`),
  KEY `inx_tpca_categroy` (`CATEGROY`),
  KEY `inx_tpca_atom_status` (`ATOM_STATUS`),
  KEY `inx_tpca_latest_flag` (`LATEST_FLAG`),
  KEY `inx_tpca_default_flag` (`DEFAULT_FLAG`),
  KEY `inx_tpca_atom_classify_id` (`CLASSIFY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子表';

-- ----------------------------
-- Table structure for T_ATOM_BUILD_APP_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_BUILD_APP_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `BUILD_INFO_ID` varchar(32) NOT NULL,
  `APP_VERSION_ID` int(11) DEFAULT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tabar_build_info_id` (`BUILD_INFO_ID`),
  KEY `inx_tabar_app_version_id` (`APP_VERSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建与编译环境关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_BUILD_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_BUILD_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `LANGUAGE` varchar(64) DEFAULT NULL,
  `SCRIPT` text NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `REPOSITORY_PATH` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tabi_language` (`LANGUAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建信息表';

-- ----------------------------
-- Table structure for T_ATOM_ENV_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_ENV_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_ID` varchar(32) NOT NULL,
  `PKG_PATH` varchar(1024) NOT NULL,
  `LANGUAGE` varchar(64) DEFAULT NULL,
  `MIN_VERSION` varchar(20) DEFAULT NULL,
  `TARGET` varchar(256) NOT NULL,
  `SHA_CONTENT` varchar(1024) DEFAULT NULL,
  `PRE_CMD` text,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PKG_NAME` varchar(256) DEFAULT '',
  PRIMARY KEY (`ID`),
  KEY `inx_tpaei_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子执行环境信息表';

-- ----------------------------
-- Table structure for T_ATOM_FEATURE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_FEATURE` (
  `ID` varchar(32) NOT NULL,
  `ATOM_CODE` varchar(64) NOT NULL,
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_taf_code` (`ATOM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子插件特性信息表';

-- ----------------------------
-- Table structure for T_ATOM_LABEL_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `LABEL_ID` varchar(32) NOT NULL,
  `ATOM_ID` varchar(32) NOT NULL DEFAULT '',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_talr_label_id` (`LABEL_ID`),
  KEY `inx_talr_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子与标签关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_OFFLINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_OFFLINE` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_CODE` varchar(64) NOT NULL,
  `BUFFER_DAY` tinyint(4) NOT NULL,
  `EXPIRE_TIME` datetime NOT NULL,
  `STATUS` tinyint(4) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tao_atom_code` (`ATOM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子下架表';

-- ----------------------------
-- Table structure for T_ATOM_OPERATE_LOG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_OPERATE_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_ID` varchar(32) NOT NULL,
  `CONTENT` text NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tpaol_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子操作日志表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_BUILD_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_PIPELINE_BUILD_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `BUILD_ID` varchar(34) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tapbr_atom_id` (`ATOM_ID`),
  KEY `inx_tapbr_pipeline_id` (`PIPELINE_ID`),
  KEY `inx_tapbr_build_id` (`BUILD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_PIPELINE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_CODE` varchar(64) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tapr_atom_code` (`ATOM_CODE`),
  KEY `inx_tapr_pipeline_id` (`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子与流水线关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_VERSION_LOG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_VERSION_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `ATOM_ID` varchar(32) NOT NULL,
  `RELEASE_TYPE` tinyint(4) NOT NULL,
  `CONTENT` text NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tpavl_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子版本日志表';

-- ----------------------------
-- Table structure for T_BUILD_RESOURCE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_BUILD_RESOURCE` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `BUILD_RESOURCE_CODE` varchar(30) NOT NULL,
  `BUILD_RESOURCE_NAME` varchar(45) NOT NULL,
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpbr_code` (`BUILD_RESOURCE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建资源信息表';

-- ----------------------------
-- Table structure for T_CATEGORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CATEGORY` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `CATEGORY_CODE` varchar(32) NOT NULL,
  `CATEGORY_NAME` varchar(32) NOT NULL,
  `ICON_URL` varchar(256) DEFAULT NULL,
  `TYPE` tinyint(4) NOT NULL DEFAULT '0',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tc_name_type` (`CATEGORY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_tc_code_code` (`CATEGORY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范畴信息表';

-- ----------------------------
-- Table structure for T_CLASSIFY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CLASSIFY` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `CLASSIFY_CODE` varchar(32) NOT NULL,
  `CLASSIFY_NAME` varchar(32) NOT NULL,
  `WEIGHT` int(11) DEFAULT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`CLASSIFY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`CLASSIFY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子分类信息表';

-- ----------------------------
-- Table structure for T_CONTAINER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTAINER` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `NAME` varchar(45) NOT NULL,
  `TYPE` varchar(20) NOT NULL,
  `OS` varchar(15) NOT NULL,
  `REQUIRED` tinyint(4) NOT NULL DEFAULT '0',
  `MAX_QUEUE_MINUTES` int(11) DEFAULT '60',
  `MAX_RUNNING_MINUTES` int(11) DEFAULT '600',
  `PROPS` text,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpc_name` (`NAME`),
  KEY `inx_tpc_os` (`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建容器表（这里的容器与Docker不是同一个概念，而是流水线模型中的一个元素）';

-- ----------------------------
-- Table structure for T_CONTAINER_RESOURCE_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTAINER_RESOURCE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `CONTAINER_ID` varchar(32) NOT NULL,
  `RESOURCE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tpcrr_container_id` (`CONTAINER_ID`),
  KEY `inx_tpcrr_resource_id` (`RESOURCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线容器与构建资源关联关系表';

-- ----------------------------
-- Table structure for T_LABEL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LABEL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `LABEL_CODE` varchar(32) NOT NULL,
  `LABEL_NAME` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`LABEL_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`LABEL_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子标签信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_ID` varchar(32) NOT NULL,
  `STORE_CODE` varchar(64) NOT NULL,
  `COMMENT_CONTENT` text NOT NULL,
  `COMMENTER_DEPT` varchar(200) NOT NULL,
  `SCORE` int(11) NOT NULL,
  `PRAISE_COUNT` int(11) DEFAULT '0',
  `PROFILE_URL` varchar(256) DEFAULT NULL,
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tsc_id` (`STORE_ID`),
  KEY `inx_tsc_code` (`STORE_CODE`),
  KEY `inx_tsc_type` (`STORE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_PRAISE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT_PRAISE` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `COMMENT_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tscp_id_creator` (`COMMENT_ID`,`CREATOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论点赞信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_REPLY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT_REPLY` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `COMMENT_ID` varchar(32) NOT NULL,
  `REPLY_CONTENT` text,
  `PROFILE_URL` varchar(256) DEFAULT NULL,
  `REPLY_TO_USER` varchar(50) DEFAULT NULL,
  `REPLYER_DEPT` varchar(200) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tscr_comment_id` (`COMMENT_ID`),
  KEY `inx_tscr_user` (`REPLY_TO_USER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论回复信息表';

-- ----------------------------
-- Table structure for T_STORE_DEPT_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_DEPT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `DEPT_ID` int(11) NOT NULL,
  `DEPT_NAME` varchar(1024) NOT NULL,
  `STATUS` tinyint(4) NOT NULL DEFAULT '0',
  `COMMENT` varchar(256) DEFAULT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `inx_tpcadr_dept_id` (`DEPT_ID`),
  KEY `inx_tsdr_code` (`STORE_CODE`),
  KEY `inx_tsdr_type` (`STORE_TYPE`),
  KEY `inx_tsdr_status` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店组件与与机构关联关系表';

-- ----------------------------
-- Table structure for T_STORE_MEMBER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_MEMBER` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `USERNAME` varchar(64) NOT NULL,
  `TYPE` tinyint(4) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tam_code_name_type` (`STORE_CODE`,`USERNAME`,`STORE_TYPE`),
  KEY `inx_tam_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件成员信息表';

-- ----------------------------
-- Table structure for T_STORE_PROJECT_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_PROJECT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `PROJECT_CODE` varchar(32) NOT NULL,
  `TYPE` tinyint(4) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tspr_code_type` (`STORE_CODE`,`PROJECT_CODE`,`STORE_TYPE`),
  KEY `inx_tpapr_project_code` (`PROJECT_CODE`),
  KEY `inx_tspr_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店组件与项目关联关系表';

-- ----------------------------
-- Table structure for T_STORE_SENSITIVE_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_SENSITIVE_CONF` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  `FIELD_NAME` varchar(64) NOT NULL DEFAULT '',
  `FIELD_VALUE` text NOT NULL,
  `FIELD_DESC` text,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tsdr_code_type_name` (`STORE_CODE`,`STORE_TYPE`,`FIELD_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_STORE_STATISTICS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_STATISTICS` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `DOWNLOADS` int(11) DEFAULT NULL,
  `COMMITS` int(11) DEFAULT NULL,
  `SCORE` int(11) DEFAULT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tss_id_code_type` (`STORE_ID`,`STORE_CODE`,`STORE_TYPE`),
  KEY `ATOM_CODE` (`STORE_CODE`),
  KEY `inx_tss_id` (`STORE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store统计信息表';

-- ----------------------------
-- Table structure for T_STORE_STATISTICS_TOTAL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_STATISTICS_TOTAL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0',
  `DOWNLOADS` int(11) DEFAULT '0',
  `COMMITS` int(11) DEFAULT '0',
  `SCORE` int(11) DEFAULT '0',
  `SCORE_AVERAGE` decimal(3,1) DEFAULT '0.0',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tss_code_type` (`STORE_CODE`,`STORE_TYPE`),
  KEY `inx_tss_downloads` (`DOWNLOADS`),
  KEY `inx_tss_comments` (`COMMITS`),
  KEY `inx_tss_score` (`SCORE`),
  KEY `inx_tss_scoreAverage` (`SCORE_AVERAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store全量统计信息表';

-- ----------------------------
-- Table structure for T_TEMPLATE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TEMPLATE` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `TEMPLATE_NAME` varchar(200) NOT NULL,
  `TEMPLATE_CODE` varchar(32) NOT NULL,
  `CLASSIFY_ID` varchar(32) NOT NULL,
  `VERSION` varchar(20) NOT NULL,
  `TEMPLATE_TYPE` tinyint(4) NOT NULL DEFAULT '1',
  `TEMPLATE_RD_TYPE` tinyint(4) NOT NULL DEFAULT '1',
  `TEMPLATE_STATUS` tinyint(4) NOT NULL,
  `TEMPLATE_STATUS_MSG` varchar(1024) DEFAULT NULL,
  `LOGO_URL` varchar(256) DEFAULT NULL,
  `SUMMARY` varchar(256) DEFAULT NULL,
  `DESCRIPTION` text,
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system',
  `PUB_DESCRIPTION` text,
  `PUBLIC_FLAG` bit(1) NOT NULL DEFAULT b'0',
  `LATEST_FLAG` bit(1) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tt_code_version` (`TEMPLATE_CODE`,`VERSION`),
  KEY `inx_tt_template_name` (`TEMPLATE_NAME`),
  KEY `inx_tt_template_code` (`TEMPLATE_CODE`),
  KEY `inx_tt_template_type` (`TEMPLATE_TYPE`),
  KEY `inx_tt_template_rd_type` (`TEMPLATE_RD_TYPE`),
  KEY `inx_tt_status` (`TEMPLATE_STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板信息表';

-- ----------------------------
-- Table structure for T_TEMPLATE_CATEGORY_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TEMPLATE_CATEGORY_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `CATEGORY_ID` varchar(32) NOT NULL,
  `TEMPLATE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_ttcr_category_id` (`CATEGORY_ID`),
  KEY `inx_ttcr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板与范畴关联关系表';

-- ----------------------------
-- Table structure for T_TEMPLATE_LABEL_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TEMPLATE_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '',
  `LABEL_ID` varchar(32) NOT NULL,
  `TEMPLATE_ID` varchar(32) NOT NULL,
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `inx_tstlr_label_id` (`LABEL_ID`),
  KEY `inx_tstlr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板与标签关联关系表';

SET FOREIGN_KEY_CHECKS = 1;
