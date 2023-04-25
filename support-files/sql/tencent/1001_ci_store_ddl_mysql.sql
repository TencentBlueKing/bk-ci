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
  `PUB_TIME` datetime DEFAULT NULL ,
  `PRIVATE_REASON` varchar(256) DEFAULT NULL ,
  `DELETE_FLAG` bit(1) DEFAULT b'0' ,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpca_code_version` (`ATOM_CODE`,`VERSION`),
  KEY `inx_tpca_service_code` (`SERVICE_SCOPE`(255)),
  KEY `inx_tpca_os` (`OS`),
  KEY `inx_tpca_atom_code` (`ATOM_CODE`),
  KEY `inx_tpca_categroy` (`CATEGROY`),
  KEY `inx_tpca_atom_status` (`ATOM_STATUS`),
  KEY `inx_tpca_latest_flag` (`LATEST_FLAG`),
  KEY `inx_tpca_default_flag` (`DEFAULT_FLAG`),
  KEY `inx_tpca_atom_classify_id` (`CLASSIFY_ID`),
  KEY `inx_ta_delete_flag` (`DELETE_FLAG`)
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
  `SAMPLE_PROJECT_PATH` varchar(500) NOT NULL DEFAULT '' COMMENT '样例工程路径',
  `ENABLE` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用  1 启用  0 禁用',
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
  `POST_ENTRY_PARAM` VARCHAR(64),
  `POST_CONDITION` VARCHAR(1024),
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
  `RECOMMEND_FLAG` bit(1) DEFAULT b'1',
  `PRIVATE_REASON` varchar(256) DEFAULT NULL,
  `DELETE_FLAG` bit(1) DEFAULT b'0',
  `YAML_FLAG` bit(1) DEFAULT b'0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_taf_code` (`ATOM_CODE`),
  KEY `inx_taf_delete_flag` (`DELETE_FLAG`),
  KEY `inx_taf_yml_flag` (`YAML_FLAG`)
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
  UNIQUE KEY `uni_inx_tavl_atom_id` (ATOM_ID)
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


CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `SERVICE_NAME` varchar(64) NOT NULL COMMENT '扩展服务名称',
  `SERVICE_CODE` varchar(64) NOT NULL COMMENT '扩展服务代码',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `SERVICE_STATUS` tinyint(4) NOT NULL COMMENT '扩展服务状态，0：初始化|1：提交中|2：构建中|3：构建失败|4：部署中|5：部署失败|6：测试中|7：提交资料|8：审核中|9：审核驳回|10：正式发布部署中|11：正式发布部署失败|12：已发布|13：上架中止|14：下架中|15：已下架',
  `SERVICE_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `ICON` text COMMENT '扩展服务图标(BASE64字符串)',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT '扩展服务简介',
  `DESCRIPTION` text COMMENT '扩展服务描述',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '扩展服务发布者',
  `PUB_TIME` datetime(3) DEFAULT NULL COMMENT '发布时间',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本扩展服务， TRUE：最新 FALSE：非最新',
  `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '删除标识 true：是，false：否',
  `OWNER` varchar(50) NOT NULL DEFAULT '' COMMENT '扩展服务归属人',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tes_code_version` (`SERVICE_CODE`,`VERSION`),
  KEY `inx_tes_service_name` (`SERVICE_NAME`),
  KEY `inx_tes_service_code` (`SERVICE_CODE`),
  KEY `inx_tes_service_status` (`SERVICE_STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务信息表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_CATEGORY_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `CATEGORY_ID` varchar(32) NOT NULL COMMENT '扩展服务范畴ID',
  `SERVICE_ID` varchar(32) NOT NULL COMMENT '扩展服务ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tescr_category_id` (`CATEGORY_ID`),
  KEY `inx_tescr_service_id` (`SERVICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='扩展服务与范畴关联关系表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_ENV_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `SERVICE_ID` varchar(32) NOT NULL COMMENT '扩展服务ID',
  `LANGUAGE` varchar(64) NOT NULL DEFAULT '' COMMENT '扩展服务开发语言',
  `PKG_PATH` varchar(1024) NOT NULL DEFAULT '' COMMENT '扩展服务执行包路径',
  `PKG_SHA_CONTENT` varchar(1024) NOT NULL DEFAULT '' COMMENT '扩展服务执行包SHA签名串',
  `DOCKER_FILE_CONTENT` text NOT NULL COMMENT 'dockefile内容',
  `IMAGE_PATH` varchar(1024) NOT NULL DEFAULT '' COMMENT '扩展服务镜像路径',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tesei_service_id` (`SERVICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务执行环境信息表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_FEATURE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `SERVICE_CODE` varchar(64) NOT NULL COMMENT '扩展服务代码',
  `SERVICE_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '扩展服务类型：0：官方自研，1：第三方',
  `PUBLIC_FLAG` bit(1) DEFAULT b'0' COMMENT '是否为公共扩展服务， TRUE：是 FALSE：不是',
  `RECOMMEND_FLAG` bit(1) DEFAULT b'1' COMMENT '是否推荐， TRUE：是 FALSE：不是',
  `CERTIFICATION_FLAG` bit(1) DEFAULT b'0' COMMENT '是否官方认证， TRUE：是 FALSE：不是',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
  `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '删除标识 true：是，false：否',
  `VISIBILITY_LEVEL` int(11) DEFAULT '0' COMMENT '扩展服务可见范围 0：私有 10：登录用户开源',
  `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '代码库hashId',
  `CODE_SRC` varchar(256) DEFAULT NULL COMMENT '代码库地址',
  `DESC_INPUT_TYPE` varchar(16) NOT NULL DEFAULT 'FILE' COMMENT '描述录入类型 FILE:文件读取 MANUAL:手工录入',
  `KILL_GRAY_APP_FLAG` bit(1) DEFAULT NULL COMMENT '是否停掉灰度环境应用， TRUE：是 FALSE：否',
  `KILL_GRAY_APP_MARK_TIME` datetime(3) DEFAULT NULL COMMENT '停掉灰度环境应用标记时间',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tesf_service_code` (`SERVICE_CODE`),
  KEY `inx_tesf_public_flag` (`PUBLIC_FLAG`),
  KEY `inx_tesf_recommend_flag` (`RECOMMEND_FLAG`),
  KEY `inx_tesf_certification_flag` (`CERTIFICATION_FLAG`),
  KEY `inx_tesf_kill_gray_app_flag` (`KILL_GRAY_APP_FLAG`),
  KEY `inx_tesf_kill_gray_app_mark_time` (`KILL_GRAY_APP_MARK_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务特性表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_ITEM_REL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ITEM_ID` varchar(32) NOT NULL COMMENT '服务功能项ID',
  `SERVICE_ID` varchar(32) NOT NULL COMMENT '扩展服务ID',
  `PROPS` text COMMENT '扩展服务前端针对该扩展点的配置信息Json串',
  `BK_SERVICE_ID` bigint(20) NOT NULL DEFAULT '0' COMMENT '蓝盾服务ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tesir_item_id` (`ITEM_ID`),
  KEY `inx_tesir_service_id` (`SERVICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务与服务功能项关联关系表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_LABEL_REL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '标签ID',
  `SERVICE_ID` varchar(32) NOT NULL COMMENT '扩展服务ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_teslr_label_id` (`LABEL_ID`),
  KEY `inx_teslr_service_id` (`SERVICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务与标签关联关系表';

CREATE TABLE IF NOT EXISTS `T_EXTENSION_SERVICE_VERSION_LOG` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `SERVICE_ID` varchar(32) NOT NULL COMMENT '扩展服务ID',
  `RELEASE_TYPE` tinyint(4) NOT NULL COMMENT '发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正',
  `CONTENT` text NOT NULL COMMENT '版本日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tesvl_service_id` (`SERVICE_ID`),
  KEY `inx_tesvl_release_type` (`RELEASE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='扩展服务版本日志表';

CREATE TABLE IF NOT EXISTS `T_IDE_ATOM` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ATOM_NAME` varchar(64) NOT NULL COMMENT 'IDE插件名称',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT 'IDE插件代码',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `ATOM_STATUS` tinyint(4) NOT NULL COMMENT 'IDE插件状态，0：初始化|1：审核中|2：审核驳回|3：已发布|4：上架中止|5：已下架',
  `ATOM_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT 'IDE插件简介',
  `DESCRIPTION` text COMMENT 'IDE插件描述',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT 'IDE插件发布者',
  `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本IDE插件， TRUE：最新 FALSE：非最新',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `VISIBILITY_LEVEL` int(11) DEFAULT '10' COMMENT 'IDE插件代码库可见范围 0：私有 10：登录用户开源',
  `PRIVATE_REASON` varchar(256) DEFAULT NULL COMMENT '不开源原因',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `uni_inx_tia_code_version` (`ATOM_CODE`,`VERSION`) USING BTREE,
  KEY `inx_tia_atom_name` (`ATOM_NAME`) USING BTREE,
  KEY `inx_tia_atom_code` (`ATOM_CODE`) USING BTREE,
  KEY `inx_tia_atom_status` (`ATOM_STATUS`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件信息表';

CREATE TABLE IF NOT EXISTS `T_IDE_ATOM_CATEGORY_REL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `CATEGORY_ID` varchar(32) NOT NULL COMMENT '范畴ID',
  `ATOM_ID` varchar(64) NOT NULL COMMENT '插件ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiacr_category_id` (`CATEGORY_ID`) USING BTREE,
  KEY `inx_tiacr_atom_id` (`ATOM_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件与范畴关联关系表';


CREATE TABLE IF NOT EXISTS `T_IDE_ATOM_ENV_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT 'IDE插件ID',
  `PKG_PATH` varchar(1024) NOT NULL COMMENT '安装包路径',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiaei_atom_id` (`ATOM_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件环境信息表';

CREATE TABLE IF NOT EXISTS `T_IDE_ATOM_FEATURE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT 'IDE插件代码',
  `ATOM_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'IDE插件类型，0：蓝盾 1：第三方',
  `PUBLIC_FLAG` bit(1) DEFAULT b'0' COMMENT '是否为公共IDE插件， TRUE：是 FALSE：不是',
  `RECOMMEND_FLAG` bit(1) DEFAULT b'1' COMMENT '是否推荐， TRUE：是 FALSE：不是',
  `CODE_SRC` varchar(256) DEFAULT NULL COMMENT '代码库地址',
  `NAMESPACE_PATH` varchar(64) DEFAULT NULL COMMENT '代码库命名空间',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiaaf_atom_code` (`ATOM_CODE`) USING BTREE,
  KEY `inx_tiaaf_atom_type` (`ATOM_TYPE`) USING BTREE,
  KEY `inx_tiaaf_public_flag` (`PUBLIC_FLAG`) USING BTREE,
  KEY `inx_tiaaf_recommend_flag` (`RECOMMEND_FLAG`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件特性表';

CREATE TABLE IF NOT EXISTS `T_IDE_ATOM_LABEL_REL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '标签ID',
  `ATOM_ID` varchar(32) NOT NULL COMMENT 'IDE插件ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tialr_label_id` (`LABEL_ID`) USING BTREE,
  KEY `inx_tialr_atom_id` (`ATOM_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件与标签关联关系表';

CREATE TABLE IF NOT EXISTS `T_IDE_ATOM_VERSION_LOG` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT 'IDE插件ID',
  `RELEASE_TYPE` tinyint(4) NOT NULL COMMENT '发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正',
  `CONTENT` text NOT NULL COMMENT '版本日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiavl_atom_id` (`ATOM_ID`) USING BTREE,
  KEY `inx_tiavl_release_type` (`RELEASE_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='IDE插件版本日志表';

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
  UNIQUE KEY `uni_inx_tspr_code_type` (`STORE_CODE`,`PROJECT_CODE`,`TYPE`,`STORE_TYPE`,`CREATOR`),
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
  `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间',
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

CREATE TABLE IF NOT EXISTS `T_ATOM_APPROVE_REL`
(
    `ID`                varchar(32) NOT NULL,
    `ATOM_CODE`         varchar(64) NOT NULL,
    `TEST_PROJECT_CODE` varchar(32) NOT NULL,
    `APPROVE_ID`        varchar(32) NOT NULL,
    `CREATOR`           varchar(50) NOT NULL DEFAULT 'system',
    `MODIFIER`          varchar(50) NOT NULL DEFAULT 'system',
    `CREATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_taar_atom_code` (`ATOM_CODE`),
    KEY `inx_taar_approve_id` (`APPROVE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS `T_ATOM_DEV_LANGUAGE_ENV_VAR`
(
    `ID`              varchar(32)  NOT NULL COMMENT '主键',
    `LANGUAGE`        varchar(64)  NOT NULL COMMENT '插件开发语言',
    `ENV_KEY`         varchar(64)  NOT NULL COMMENT '环境变量key值',
    `ENV_VALUE`       varchar(256) NOT NULL COMMENT '环境变量value值',
    `BUILD_HOST_TYPE` varchar(32)  NOT NULL COMMENT '适用构建机类型 PUBLIC:公共构建机，THIRD:第三方构建机，ALL:所有',
    `BUILD_HOST_OS`   varchar(32)  NOT NULL COMMENT '适用构建机操作系统 WINDOWS:windows构建机，LINUX:linux构建机，MAC_OS:mac构建机，ALL:所有',
    `CREATOR`         varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`        varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `inx_taev_language` (`LANGUAGE`),
    KEY `inx_taev_key` (`ENV_KEY`),
    KEY `inx_taev_host_type` (`BUILD_HOST_TYPE`),
    KEY `inx_taev_host_os` (`BUILD_HOST_OS`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='插件环境变量信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_OPT_LOG`
(
    `ID`          varchar(32)  NOT NULL DEFAULT '',
    `STORE_CODE`  varchar(64)  NOT NULL DEFAULT '',
    `STORE_TYPE`  tinyint(4)   NOT NULL DEFAULT '0',
    `OPT_TYPE`    varchar(64)  NOT NULL DEFAULT '',
    `OPT_DESC`    varchar(512) NOT NULL DEFAULT '',
    `OPT_USER`    varchar(64)  NOT NULL DEFAULT '',
    `OPT_TIME`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `store_item` (`STORE_CODE`, `STORE_TYPE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_STORE_APPROVE`
(
    `ID`          varchar(32) NOT NULL,
    `STORE_CODE`  varchar(64) NOT NULL,
    `STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0',
    `CONTENT`     text        NOT NULL,
    `APPLICANT`   varchar(50) NOT NULL,
    `TYPE`        varchar(64)          DEFAULT NULL,
    `STATUS`      varchar(64)          DEFAULT NULL,
    `APPROVER`    varchar(50)          DEFAULT '',
    `APPROVE_MSG` varchar(256)         DEFAULT '',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_tsa_applicant` (`APPLICANT`),
    KEY `inx_tsa_type` (`TYPE`),
    KEY `inx_tsa_status` (`STATUS`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_STORE_RELEASE`
(
    `ID`                  varchar(32) NOT NULL COMMENT '主键',
    `STORE_CODE`          varchar(64) NOT NULL COMMENT 'store组件代码',
    `FIRST_PUB_CREATOR`   varchar(64) NOT NULL COMMENT '首次发布人',
    `FIRST_PUB_TIME`      datetime    NOT NULL COMMENT '首次发布时间',
    `LATEST_UPGRADER`     varchar(64) NOT NULL COMMENT '最近升级人',
    `LATEST_UPGRADE_TIME` datetime    NOT NULL COMMENT '最近升级时间',
    `STORE_TYPE`          tinyint(4)  NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件',
    `CREATOR`             varchar(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`            varchar(64) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_inx_tsr_code_type` (`STORE_CODE`, `STORE_TYPE`),
    KEY `inx_tsr_f_pub_creater` (`FIRST_PUB_CREATOR`),
    KEY `inx_tsr_f_pub_time` (`FIRST_PUB_TIME`),
    KEY `inx_tsr_latest_upgrader` (`LATEST_UPGRADER`),
    KEY `inx_tsr_latest_upgrade_time` (`LATEST_UPGRADE_TIME`),
    KEY `inx_tsr_code` (`STORE_CODE`),
    KEY `inx_tsr_type` (`STORE_TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='store组件发布升级信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_PIPELINE_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `STORE_CODE`  varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板 2：镜像 3：IDE插件',
    PRIMARY KEY (`ID`),
    KEY `inx_tspr_code` (`STORE_CODE`),
    KEY `inx_tspr_type` (`STORE_TYPE`),
    KEY `inx_tspr_pipeline_id` (`PIPELINE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商店组件与与流水线关联关系表';

CREATE TABLE IF NOT EXISTS `T_STORE_PIPELINE_BUILD_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `STORE_ID`    varchar(32) NOT NULL DEFAULT '' COMMENT '商店组件ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `BUILD_ID`    varchar(34) NOT NULL COMMENT '构建ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `inx_tspbr_atom_id` (`STORE_ID`),
    KEY `inx_tspbr_pipeline_id` (`PIPELINE_ID`),
    KEY `inx_tspbr_build_id` (`BUILD_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商店组件构建关联关系表';

CREATE TABLE IF NOT EXISTS `T_LOGO`
(
    `ID`          varchar(32)  NOT NULL,
    `TYPE`        varchar(16)  NOT NULL,
    `LOGO_URL`    varchar(512) NOT NULL DEFAULT '',
    `LINK`        varchar(512)          DEFAULT '',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system',
    `MODIFIER`    varchar(50)  NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ORDER`       int(8)       NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REASON`
(
    `ID`          varchar(32)  NOT NULL COMMENT '主键',
    `TYPE`        varchar(32)  NOT NULL COMMENT '类型',
    `CONTENT`     varchar(512) NOT NULL DEFAULT '',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `ENABLE`      bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `ORDER`       int(8)       NOT NULL DEFAULT '0' COMMENT '显示顺序',
    PRIMARY KEY (`ID`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='原因定义表';

CREATE TABLE IF NOT EXISTS `T_REASON_REL`
(
   `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
   `TYPE` varchar(32) NOT NULL COMMENT '类型',
   `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '商城组件编码',
   `REASON_ID` varchar(32) NOT NULL COMMENT '原因ID',
   `NOTE` text COMMENT '原因说明',
   `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
   `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'STORE组件类型',
   PRIMARY KEY (`ID`) USING BTREE,
   KEY `store_code` (`STORE_CODE`) USING BTREE,
   KEY `type` (`TYPE`) USING BTREE,
   KEY `inx_trr_store_type` (`STORE_TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='原因和组件关联关系';

CREATE TABLE IF NOT EXISTS `T_IMAGE`
(
    `ID`                varchar(32)  NOT NULL COMMENT '主键',
    `IMAGE_NAME`        varchar(64)  NOT NULL COMMENT '镜像名称',
    `IMAGE_CODE`        varchar(64)  NOT NULL COMMENT '镜像代码',
    `CLASSIFY_ID`       varchar(32)  NOT NULL COMMENT '所属分类ID',
    `VERSION`           varchar(20)  NOT NULL COMMENT '版本号',
    `IMAGE_SOURCE_TYPE` varchar(20)  NOT NULL DEFAULT 'bkdevops' COMMENT '镜像来源，bkdevops：蓝盾源 third：第三方源',
    `IMAGE_REPO_URL`    varchar(256)          DEFAULT NULL COMMENT '镜像仓库地址',
    `IMAGE_REPO_NAME`   varchar(256) NOT NULL COMMENT '镜像在仓库名称',
    `TICKET_ID`         varchar(256)          DEFAULT NULL COMMENT 'ticket身份ID',
    `IMAGE_STATUS`      tinyint(4)   NOT NULL COMMENT '镜像状态，0：初始化|1：提交中|2：验证中|3：验证失败|4：测试中|5：审核中|6：审核驳回|7：已发布|8：上架中止|9：下架中|10：已下架',
    `IMAGE_STATUS_MSG`  varchar(1024)         DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
    `IMAGE_SIZE`        varchar(20)  NOT NULL COMMENT '镜像大小',
    `IMAGE_TAG`         varchar(256)          DEFAULT NULL COMMENT '镜像tag',
    `LOGO_URL`          varchar(256)          DEFAULT NULL COMMENT 'logo地址',
    `ICON`              text COMMENT '镜像图标(BASE64字符串)',
    `SUMMARY`           varchar(256)          DEFAULT NULL COMMENT '镜像简介',
    `DESCRIPTION`       text COMMENT '镜像描述',
    `PUBLISHER`         varchar(50)  NOT NULL DEFAULT 'system' COMMENT '镜像发布者',
    `PUB_TIME`          datetime              DEFAULT NULL COMMENT '发布时间',
    `LATEST_FLAG`       bit(1)       NOT NULL COMMENT '是否为最新版本镜像， TRUE：最新 FALSE：非最新',
    `CREATOR`           varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`          varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `AGENT_TYPE_SCOPE`  varchar(64)  NOT NULL DEFAULT '[]' COMMENT '支持的构建机环境',
    `DELETE_FLAG`       BIT(1)       DEFAULT FALSE  COMMENT '删除标识 true：是，false：否',
    `DOCKER_FILE_TYPE` varchar(32) NOT NULL DEFAULT 'INPUT' COMMENT 'dockerFile类型（INPUT：手动输入，*_LINK：链接）',
    `DOCKER_FILE_CONTENT` text COMMENT 'dockerFile内容',
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `uni_inx_ti_code_version` (`IMAGE_CODE`, `VERSION`) USING BTREE,
    KEY `inx_ti_image_name` (`IMAGE_NAME`) USING BTREE,
    KEY `inx_ti_image_code` (`IMAGE_CODE`) USING BTREE,
    KEY `inx_ti_source_type` (`IMAGE_SOURCE_TYPE`) USING BTREE,
    KEY `inx_ti_image_status` (`IMAGE_STATUS`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='镜像信息表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_CATEGORY_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `CATEGORY_ID` varchar(32) NOT NULL COMMENT '镜像范畴ID',
    `IMAGE_ID`    varchar(32) NOT NULL COMMENT '镜像ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_ticr_category_id` (`CATEGORY_ID`) USING BTREE,
    KEY `inx_ticr_image_id` (`IMAGE_ID`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像与范畴关联关系表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_FEATURE`
(
    `ID`                 varchar(32)  NOT NULL COMMENT '主键',
    `IMAGE_CODE`         varchar(256) NOT NULL COMMENT '镜像代码',
    `PUBLIC_FLAG`        bit(1)                DEFAULT b'0' COMMENT '是否为公共镜像， TRUE：是 FALSE：不是',
    `RECOMMEND_FLAG`     bit(1)                DEFAULT b'1' COMMENT '是否推荐， TRUE：是 FALSE：不是',
    `CREATOR`            varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`           varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CERTIFICATION_FLAG` bit(1)                DEFAULT b'0' COMMENT '是否官方认证， TRUE：是 FALSE：不是',
    `WEIGHT`             int(11)               DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
    `IMAGE_TYPE`         tinyint(4)   NOT NULL DEFAULT '1' COMMENT '镜像类型：0：蓝鲸官方，1：第三方',
    `DELETE_FLAG`        BIT(1)       DEFAULT FALSE  COMMENT '删除标识 true：是，false：否',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_tif_image_code` (`IMAGE_CODE`) USING BTREE,
    KEY `inx_tif_public_flag` (`PUBLIC_FLAG`) USING BTREE,
    KEY `inx_tif_recommend_flag` (`RECOMMEND_FLAG`) USING BTREE,
    KEY `inx_tif_certification_flag` (`CERTIFICATION_FLAG`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像特性表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_LABEL_REL`
(
    `ID`          varchar(32) NOT NULL COMMENT '主键',
    `LABEL_ID`    varchar(32) NOT NULL COMMENT '模板标签ID',
    `IMAGE_ID`    varchar(32) NOT NULL COMMENT '镜像ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_tilr_label_id` (`LABEL_ID`) USING BTREE,
    KEY `inx_tilr_image_id` (`IMAGE_ID`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像与标签关联关系表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_VERSION_LOG`
(
    `ID`           varchar(32) NOT NULL COMMENT '主键',
    `IMAGE_ID`     varchar(32) NOT NULL COMMENT '镜像ID',
    `RELEASE_TYPE` tinyint(4)  NOT NULL COMMENT '发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正',
    `CONTENT`      text        NOT NULL COMMENT '版本日志内容',
    `CREATOR`      varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `uni_inx_tivl_image_id` (`IMAGE_ID`) USING BTREE,
    KEY `inx_tivl_release_type` (`RELEASE_TYPE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像版本日志表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_AGENT_TYPE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `IMAGE_CODE` varchar(64) NOT NULL COMMENT '镜像代码',
  `AGENT_TYPE` varchar(32) NOT NULL COMMENT '机器类型 PUBLIC_DEVNET，PUBLIC_IDC，PUBLIC_DEVCLOUD',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiat_agent_type` (`AGENT_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '镜像与机器类型关联表';

CREATE TABLE IF NOT EXISTS `T_BUSINESS_CONFIG` (
  `BUSINESS` varchar(64) NOT NULL COMMENT '业务名称',
  `FEATURE` varchar(64) NOT NULL COMMENT '要控制的功能特性',
  `BUSINESS_VALUE` varchar(255) NOT NULL COMMENT '业务取值',
  `CONFIG_VALUE` text NOT NULL COMMENT '配置值',
  `DESCRIPTION` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `i_feature_business_businessValue` (`FEATURE`,`BUSINESS`,`BUSINESS_VALUE`) USING BTREE COMMENT '同一业务相同业务值的相同特性仅能有一个配置'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `T_STORE_MEDIA_INFO` (
  `ID` VARCHAR(32) COMMENT '主键',
  `STORE_CODE` VARCHAR(64)  NOT NULL  COMMENT 'store组件标识',
  `MEDIA_URL` VARCHAR(256)  NOT NULL  COMMENT '媒体资源链接',
  `MEDIA_TYPE` VARCHAR(32)  NOT NULL  COMMENT '媒体资源类型 PICTURE:图片 VIDEO:视频',
  `STORE_TYPE` TINYINT(4) NOT NULL DEFAULT 0 COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
  `CREATOR` VARCHAR(50) NOT NULL DEFAULT 'system'  COMMENT '创建人',
  `MODIFIER` VARCHAR(50)  NOT NULL DEFAULT 'system'  COMMENT '最近修改人',
  `CREATE_TIME` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tsmi_store_code` (`STORE_CODE`)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COMMENT = '媒体信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_BUILD_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `LANGUAGE` varchar(64) DEFAULT NULL COMMENT '开发语言',
  `SCRIPT` text NOT NULL COMMENT '打包脚本',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPOSITORY_PATH` varchar(500) DEFAULT NULL COMMENT '代码存放路径',
  `ENABLE` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用  1 启用  0 禁用',
  `SAMPLE_PROJECT_PATH` varchar(500) NOT NULL DEFAULT '' COMMENT '样例工程路径',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `inx_tsbi_language_type` (`LANGUAGE`,`STORE_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='store组件构建信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_CODECC` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_ID` varchar(32) NOT NULL DEFAULT '' COMMENT 'store组件ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `TASK_ID` varchar(34) NOT NULL COMMENT '代码扫描任务ID',
  `CODE_STYLE_SCORE` double(6,2) DEFAULT NULL COMMENT '代码规范评分',
  `CODE_SECURITY_SCORE` double(6,2) DEFAULT NULL COMMENT '代码安全评分',
  `CODE_MEASURE_SCORE` double(6,2) DEFAULT NULL COMMENT '代码度量和检查评分',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  PRIMARY KEY (`ID`),
  KEY `un_inx_tsc_code_type` (`STORE_CODE`,`STORE_TYPE`),
  KEY `inx_tsc_build_id` (`BUILD_ID`),
  KEY `inx_tsc_task_id` (`TASK_ID`),
  KEY `inx_tsc_id` (`STORE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store代码扫描信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_BUILD_APP_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `BUILD_INFO_ID` varchar(32) NOT NULL COMMENT '构建信息Id(对应T_STORE_BUILD_INFO主键)',
  `APP_VERSION_ID` int(11) DEFAULT NULL COMMENT '编译环境版本Id(对应T_APP_VERSION主键)',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tabar_build_info_id` (`BUILD_INFO_ID`) USING BTREE,
  KEY `inx_tabar_app_version_id` (`APP_VERSION_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='store构建与编译环境关联关系表';

CREATE TABLE IF NOT EXISTS `T_STORE_ENV_VAR` (
  `ID` VARCHAR(32) NOT NULL COMMENT '主键',
  `STORE_CODE` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '组件编码',
  `STORE_TYPE` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
  `VAR_NAME` VARCHAR(64) NOT NULL COMMENT '变量名',
  `VAR_VALUE` TEXT NOT NULL COMMENT '变量值',
  `VAR_DESC` TEXT COMMENT '描述',
  `SCOPE` VARCHAR(16) NOT NULL COMMENT '生效范围 TEST：测试 PRD：正式 ALL：所有',
  `ENCRYPT_FLAG` BIT(1) DEFAULT FALSE  COMMENT '是否加密， TRUE：是 FALSE：否',
  `VERSION` INT(11) NOT NULL DEFAULT 1  COMMENT '版本号',
  `CREATOR` VARCHAR(50) NOT NULL DEFAULT 'system'  COMMENT '创建人',
  `MODIFIER` VARCHAR(50)  NOT NULL DEFAULT 'system'  COMMENT '最近修改人',
  `CREATE_TIME` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
   PRIMARY KEY (`ID`),
   KEY `INX_TESV_SCOPE` (`SCOPE`),
   KEY `INX_TESV_ENCRYPT_FLAG` (`ENCRYPT_FLAG`),
   UNIQUE KEY `UNI_INX_TSEV_CODE_TYPE_NAME_VERSION` (`STORE_CODE`,`STORE_TYPE`,`VAR_NAME`,`VERSION`)
   )
ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT = '环境变量信息表';

SET FOREIGN_KEY_CHECKS = 1;
