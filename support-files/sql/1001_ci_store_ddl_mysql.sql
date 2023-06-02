USE devops_ci_store;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APPS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APPS` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '名称',
  `OS` varchar(32) NOT NULL DEFAULT '' COMMENT '操作系统',
  `BIN_PATH` varchar(64) DEFAULT NULL COMMENT '执行所在路径',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_OS` (`NAME`,`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境信息表';

-- ----------------------------
-- Table structure for T_APP_ENV
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_ENV` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `APP_ID` int(11) NOT NULL COMMENT '编译环境ID',
  `PATH` varchar(32) NOT NULL DEFAULT '' COMMENT '路径',
  `NAME` varchar(32) NOT NULL DEFAULT '' COMMENT '名称',
  `DESCRIPTION` varchar(64) NOT NULL DEFAULT '' COMMENT '描述',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境变量表';

-- ----------------------------
-- Table structure for T_APP_VERSION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_VERSION` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `APP_ID` int(11) NOT NULL COMMENT '编译环境ID',
  `VERSION` varchar(32) DEFAULT '' COMMENT '版本号',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `APP_ID` (`APP_ID`,`VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编译环境版本信息表';

-- ----------------------------
-- Table structure for T_ATOM
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `NAME` varchar(64) NOT NULL COMMENT '名称',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '插件的唯一标识',
  `CLASS_TYPE` varchar(64) NOT NULL COMMENT '插件大类',
  `SERVICE_SCOPE` varchar(256) NOT NULL COMMENT '生效范围',
  `JOB_TYPE` varchar(20) DEFAULT NULL COMMENT '适用Job类型，AGENT： 编译环境，AGENT_LESS：无编译环境',
  `OS` varchar(100) NOT NULL COMMENT '操作系统',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `DOCS_LINK` varchar(256) DEFAULT NULL COMMENT '文档跳转链接',
  `ATOM_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '原子类型',
  `ATOM_STATUS` tinyint(4) NOT NULL COMMENT '原子状态',
  `ATOM_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '插件状态信息',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT '简介',
  `DESCRIPTION` text COMMENT '描述',
  `CATEGROY` tinyint(4) NOT NULL DEFAULT '1' COMMENT '类别',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'LOGO URL地址',
  `ICON` text COMMENT '插件图标',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为默认原子',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本原子',
  `BUILD_LESS_RUN_FLAG` bit(1) DEFAULT NULL COMMENT '无构建环境原子是否可以在有构建环境运行标识',
  `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '代码库哈希ID',
  `CODE_SRC` varchar(256) DEFAULT NULL COMMENT '代码库链接',
  `PAY_FLAG` bit(1) DEFAULT b'1' COMMENT '是否免费',
  `HTML_TEMPLATE_VERSION` varchar(10) NOT NULL DEFAULT '1.1' COMMENT '前端渲染模板版本',
  `PROPS` text COMMENT '自定义扩展容器前端表单属性字段的JSON串',
  `DATA` text COMMENT '预留字段',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '原子发布者',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权值',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0' COMMENT '可见范围',
  `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间',
  `PRIVATE_REASON` varchar(256) DEFAULT NULL COMMENT '插件代码库不开源原因',
  `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `BRANCH` VARCHAR(128) DEFAULT 'master' COMMENT '代码库分支',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpca_code_version` (`ATOM_CODE`,`VERSION`),
  KEY `inx_tpca_service_code` (`SERVICE_SCOPE`(255)),
  KEY `inx_tpca_os` (`OS`),
  KEY `inx_tpca_code_version_time` (`ATOM_CODE`,`VERSION`,`CREATE_TIME`),
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
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `BUILD_INFO_ID` varchar(32) NOT NULL COMMENT '构建信息Id',
  `APP_VERSION_ID` int(11) DEFAULT NULL COMMENT '编译环境版本Id(对应T_APP_VERSION主键)',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tabar_build_info_id` (`BUILD_INFO_ID`),
  KEY `inx_tabar_app_version_id` (`APP_VERSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建与编译环境关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_BUILD_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_BUILD_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `LANGUAGE` varchar(64) DEFAULT NULL COMMENT '语言',
  `SCRIPT` text NOT NULL COMMENT '打包脚本',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPOSITORY_PATH` varchar(500) DEFAULT NULL COMMENT '代码存放路径',
  `SAMPLE_PROJECT_PATH` varchar(500) NOT NULL DEFAULT '' COMMENT '样例工程路径',
  `ENABLE` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用  1 启用  0 禁用',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tabi_language` (`LANGUAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建信息表';

-- ----------------------------
-- Table structure for T_ATOM_ENV_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_ENV_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '插件Id',
  `PKG_PATH` varchar(1024) NOT NULL COMMENT '安装包路径',
  `LANGUAGE` varchar(64) DEFAULT NULL COMMENT '语言',
  `MIN_VERSION` varchar(20) DEFAULT NULL COMMENT '支持插件开发语言的最低版本',
  `TARGET` varchar(256) NOT NULL COMMENT '插件执行入口',
  `SHA_CONTENT` varchar(1024) DEFAULT NULL COMMENT '插件SHA签名串',
  `PRE_CMD` text COMMENT '插件执行前置命令',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `PKG_NAME` varchar(256) DEFAULT '' COMMENT '插件包名',
  `POST_ENTRY_PARAM` VARCHAR(64) COMMENT '入口参数',
  `POST_CONDITION` VARCHAR(1024) COMMENT '执行条件',
  `OS_NAME` varchar(128) DEFAULT NULL COMMENT '支持的操作系统名称',
  `OS_ARCH` varchar(128) DEFAULT NULL COMMENT '支持的操作系统架构',
  `RUNTIME_VERSION` varchar(128) DEFAULT NULL COMMENT '运行时版本',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否为默认环境信息',
  `FINISH_KILL_FLAG` bit(1) COMMENT '插件运行结束后是否立即杀掉其进程',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TAEI_ID_OS_NAME_ARCH` (`ATOM_ID`,`OS_NAME`,`OS_ARCH`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子执行环境信息表';


-- ----------------------------
-- Table structure for T_ATOM_FEATURE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_FEATURE` (
  `ID` varchar(32) NOT NULL COMMENT '主键ID',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '插件的唯一标识',
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0' COMMENT '可见范围',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `RECOMMEND_FLAG` bit(1) DEFAULT b'1' COMMENT '是否推荐',
  `PRIVATE_REASON` varchar(256) DEFAULT NULL COMMENT '插件代码库不开源原因',
  `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `YAML_FLAG` bit(1) DEFAULT b'0' COMMENT 'yaml可用标识',
  `QUALITY_FLAG` bit(1) DEFAULT b'0' COMMENT '质量红线可用标识',
  `CERTIFICATION_FLAG` bit(1) DEFAULT b'0' COMMENT '是否认证标识',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_taf_code` (`ATOM_CODE`),
  KEY `inx_taf_delete_flag` (`DELETE_FLAG`),
  KEY `inx_taf_yml_flag` (`YAML_FLAG`),
  KEY `inx_taf_quality_flag` (`QUALITY_FLAG`),
  KEY `inx_taf_certification_flag` (`CERTIFICATION_FLAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子插件特性信息表';

-- ----------------------------
-- Table structure for T_ATOM_LABEL_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '标签ID',
  `ATOM_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '插件Id',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_talr_label_id` (`LABEL_ID`),
  KEY `inx_talr_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子与标签关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_OFFLINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_OFFLINE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '插件的唯一标识',
  `BUFFER_DAY` tinyint(4) NOT NULL COMMENT '',
  `EXPIRE_TIME` datetime NOT NULL COMMENT '过期时间',
  `STATUS` tinyint(4) NOT NULL COMMENT '状态',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tao_atom_code` (`ATOM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子下架表';

-- ----------------------------
-- Table structure for T_ATOM_OPERATE_LOG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_OPERATE_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '插件Id',
  `CONTENT` text NOT NULL COMMENT '日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tpaol_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子操作日志表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_BUILD_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_PIPELINE_BUILD_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '插件Id',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tapbr_atom_id` (`ATOM_ID`),
  KEY `inx_tapbr_pipeline_id` (`PIPELINE_ID`),
  KEY `inx_tapbr_build_id` (`BUILD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子构建关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_PIPELINE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '插件的唯一标识',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tapr_atom_code` (`ATOM_CODE`),
  KEY `inx_tapr_pipeline_id` (`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子与流水线关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_VERSION_LOG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ATOM_VERSION_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '插件Id',
  `RELEASE_TYPE` tinyint(4) NOT NULL COMMENT '发布类型',
  `CONTENT` text NOT NULL COMMENT '日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tavl_atom_id` (ATOM_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子版本日志表';

-- ----------------------------
-- Table structure for T_BUILD_RESOURCE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_BUILD_RESOURCE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `BUILD_RESOURCE_CODE` varchar(30) NOT NULL COMMENT '构建资源代码',
  `BUILD_RESOURCE_NAME` varchar(45) NOT NULL COMMENT '构建资源名称',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为默认原子',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpbr_code` (`BUILD_RESOURCE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建资源信息表';

-- ----------------------------
-- Table structure for T_CATEGORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CATEGORY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CATEGORY_CODE` varchar(32) NOT NULL COMMENT '范畴代码',
  `CATEGORY_NAME` varchar(32) NOT NULL COMMENT '范畴名称',
  `ICON_URL` varchar(256) DEFAULT NULL COMMENT '范畴图标链接',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '类型',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tc_name_type` (`CATEGORY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_tc_code_code` (`CATEGORY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范畴信息表';

-- ----------------------------
-- Table structure for T_CLASSIFY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CLASSIFY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CLASSIFY_CODE` varchar(32) NOT NULL COMMENT '所属镜像分类代码',
  `CLASSIFY_NAME` varchar(32) NOT NULL COMMENT '所属镜像分类名称',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权值',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`CLASSIFY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`CLASSIFY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线原子分类信息表';

-- ----------------------------
-- Table structure for T_CONTAINER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTAINER` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `NAME` varchar(45) NOT NULL COMMENT '名称',
  `TYPE` varchar(20) NOT NULL COMMENT '类型',
  `OS` varchar(15) NOT NULL COMMENT '操作系统',
  `REQUIRED` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否必须',
  `MAX_QUEUE_MINUTES` int(11) DEFAULT '60' COMMENT '最大排队时间',
  `MAX_RUNNING_MINUTES` int(11) DEFAULT '600' COMMENT '最大运行时间',
  `PROPS` text COMMENT '自定义扩展容器前端表单属性字段的JSON串',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpc_name` (`NAME`),
  KEY `inx_tpc_os` (`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建容器表（这里的容器与Docker不是同一个概念，而是流水线模型中的一个元素）';

-- ----------------------------
-- Table structure for T_CONTAINER_RESOURCE_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_CONTAINER_RESOURCE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CONTAINER_ID` varchar(32) NOT NULL COMMENT '构建容器ID',
  `RESOURCE_ID` varchar(32) NOT NULL COMMENT '资源ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tpcrr_container_id` (`CONTAINER_ID`),
  KEY `inx_tpcrr_resource_id` (`RESOURCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线容器与构建资源关联关系表';

-- ----------------------------
-- Table structure for T_LABEL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LABEL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `LABEL_CODE` varchar(32) NOT NULL COMMENT '镜像标签代码',
  `LABEL_NAME` varchar(32) NOT NULL COMMENT '标签名称',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`LABEL_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`LABEL_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原子标签信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_ID` varchar(32) NOT NULL COMMENT '商店组件ID',
  `STORE_CODE` varchar(64) NOT NULL COMMENT 'store组件编码',
  `COMMENT_CONTENT` text NOT NULL COMMENT '评论内容',
  `COMMENTER_DEPT` varchar(200) NOT NULL COMMENT '评论者组织架构信息',
  `SCORE` int(11) NOT NULL COMMENT '评分',
  `PRAISE_COUNT` int(11) DEFAULT '0' COMMENT '点赞个数',
  `PROFILE_URL` varchar(256) DEFAULT NULL COMMENT '评论者头像url地址',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tsc_id` (`STORE_ID`),
  KEY `inx_tsc_code` (`STORE_CODE`),
  KEY `inx_tsc_type` (`STORE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_PRAISE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT_PRAISE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `COMMENT_ID` varchar(32) NOT NULL COMMENT '评论ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tscp_id_creator` (`COMMENT_ID`,`CREATOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论点赞信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_REPLY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_COMMENT_REPLY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `COMMENT_ID` varchar(32) NOT NULL COMMENT '评论ID',
  `REPLY_CONTENT` text COMMENT '回复内容',
  `PROFILE_URL` varchar(256) DEFAULT NULL COMMENT '评论者头像url地址',
  `REPLY_TO_USER` varchar(50) DEFAULT NULL COMMENT '被回复者',
  `REPLYER_DEPT` varchar(200) NOT NULL COMMENT '回复者组织架构信息',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tscr_comment_id` (`COMMENT_ID`),
  KEY `inx_tscr_user` (`REPLY_TO_USER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件评论回复信息表';

-- ----------------------------
-- Table structure for T_STORE_DEPT_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_DEPT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `DEPT_ID` int(11) NOT NULL COMMENT '项目所属二级机构ID',
  `DEPT_NAME` varchar(1024) NOT NULL COMMENT '项目所属二级机构名称',
  `STATUS` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `COMMENT` varchar(256) DEFAULT NULL COMMENT '评论',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
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
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `USERNAME` varchar(64) NOT NULL COMMENT '用户名称',
  `TYPE` tinyint(4) NOT NULL COMMENT '类型',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tam_code_name_type` (`STORE_CODE`,`USERNAME`,`STORE_TYPE`),
  KEY `inx_tam_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store组件成员信息表';

-- ----------------------------
-- Table structure for T_STORE_PROJECT_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_PROJECT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `PROJECT_CODE` varchar(32) NOT NULL COMMENT '用户组所属项目',
  `TYPE` tinyint(4) NOT NULL COMMENT '类型',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tspr_code_type` (`STORE_CODE`,`PROJECT_CODE`,`TYPE`,`STORE_TYPE`,`CREATOR`),
  KEY `inx_tpapr_project_code` (`PROJECT_CODE`),
  KEY `inx_tspr_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店组件与项目关联关系表';

-- ----------------------------
-- Table structure for T_STORE_SENSITIVE_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_SENSITIVE_CONF` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  `FIELD_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '字段名称',
  `FIELD_VALUE` text NOT NULL COMMENT '字段值',
  `FIELD_DESC` text COMMENT '字段描述',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `FIELD_TYPE` VARCHAR(16) DEFAULT 'BACKEND' COMMENT '字段类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tsdr_code_type_name` (`STORE_CODE`,`STORE_TYPE`,`FIELD_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store私有配置表';

-- ----------------------------
-- Table structure for T_STORE_STATISTICS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_STATISTICS` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '商店组件ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `DOWNLOADS` int(11) DEFAULT NULL COMMENT '下载量',
  `COMMITS` int(11) DEFAULT NULL COMMENT '评论数量',
  `SCORE` int(11) DEFAULT NULL COMMENT '评分',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tss_id_code_type` (`STORE_ID`,`STORE_CODE`,`STORE_TYPE`),
  KEY `ATOM_CODE` (`STORE_CODE`),
  KEY `inx_tss_id` (`STORE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='store统计信息表';

-- ----------------------------
-- Table structure for T_STORE_STATISTICS_TOTAL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STORE_STATISTICS_TOTAL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  `DOWNLOADS` int(11) DEFAULT '0' COMMENT '下载量',
  `COMMITS` int(11) DEFAULT '0' COMMENT '评论数量',
  `SCORE` int(11) DEFAULT '0' COMMENT '评分',
  `SCORE_AVERAGE` decimal(3,1) DEFAULT '0.0' COMMENT '评论均分',
  `PIPELINE_NUM` INT(11) DEFAULT '0' COMMENT '流水线数量',
  `RECENT_EXECUTE_NUM` INT(11) DEFAULT '0' COMMENT '最近执行次数',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `HOT_FLAG` bit(1) DEFAULT b'0' COMMENT '是否为受欢迎组件',
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
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `TEMPLATE_NAME` varchar(200) NOT NULL COMMENT '模板名称',
  `TEMPLATE_CODE` varchar(32) NOT NULL COMMENT '模板代码',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `TEMPLATE_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '模板类型',
  `TEMPLATE_RD_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '模板研发类型',
  `TEMPLATE_STATUS` tinyint(4) NOT NULL COMMENT '模板状态',
  `TEMPLATE_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '模板状态信息',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'LOGO URL地址',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT '简介',
  `DESCRIPTION` text COMMENT '描述',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '原子发布者',
  `PUB_DESCRIPTION` text COMMENT '发布描述',
  `PUBLIC_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为公共镜像',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本原子',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
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
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CATEGORY_ID` varchar(32) NOT NULL COMMENT '镜像范畴ID',
  `TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_ttcr_category_id` (`CATEGORY_ID`),
  KEY `inx_ttcr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板与范畴关联关系表';

-- ----------------------------
-- Table structure for T_TEMPLATE_LABEL_REL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TEMPLATE_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '标签ID',
  `TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tstlr_label_id` (`LABEL_ID`),
  KEY `inx_tstlr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板与标签关联关系表';

CREATE TABLE IF NOT EXISTS `T_ATOM_APPROVE_REL`
(
    `ID`                varchar(32) NOT NULL COMMENT '主键ID',
    `ATOM_CODE`         varchar(64) NOT NULL COMMENT '插件的唯一标识',
    `TEST_PROJECT_CODE` varchar(32) NOT NULL COMMENT '调试项目编码',
    `APPROVE_ID`        varchar(32) NOT NULL COMMENT '审批ID',
    `CREATOR`           varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER`          varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `inx_taar_atom_code` (`ATOM_CODE`),
    KEY `inx_taar_approve_id` (`APPROVE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='插件审核关联关系表';


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
    `ID`          varchar(32)  NOT NULL DEFAULT '' COMMENT '主键ID',
    `STORE_CODE`  varchar(64)  NOT NULL DEFAULT '' COMMENT 'store组件编码',
    `STORE_TYPE`  tinyint(4)   NOT NULL DEFAULT '0' COMMENT 'store组件类型',
    `OPT_TYPE`    varchar(64)  NOT NULL DEFAULT '' COMMENT '操作类型',
    `OPT_DESC`    varchar(512) NOT NULL DEFAULT '' COMMENT '操作内容',
    `OPT_USER`    varchar(64)  NOT NULL DEFAULT '' COMMENT '操作用户',
    `OPT_TIME`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建者',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `store_item` (`STORE_CODE`, `STORE_TYPE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='store操作日志表';

CREATE TABLE IF NOT EXISTS `T_STORE_APPROVE`
(
    `ID`          varchar(32) NOT NULL COMMENT '主键ID',
    `STORE_CODE`  varchar(64) NOT NULL COMMENT 'store组件编码',
    `STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0' COMMENT 'store组件类型',
    `CONTENT`     text        NOT NULL COMMENT '日志内容',
    `APPLICANT`   varchar(50) NOT NULL COMMENT '申请人',
    `TYPE`        varchar(64)          DEFAULT NULL COMMENT '类型',
    `STATUS`      varchar(64)          DEFAULT NULL COMMENT '状态',
    `APPROVER`    varchar(50)          DEFAULT '' COMMENT '批准人',
    `APPROVE_MSG` varchar(256)         DEFAULT '' COMMENT '批准信息',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `TOKEN` varchar(64) DEFAULT NULL,
    PRIMARY KEY (`ID`),
    KEY `inx_tsa_applicant` (`APPLICANT`),
    KEY `inx_tsa_type` (`TYPE`),
    KEY `inx_tsa_status` (`STATUS`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='审核表';

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
    `BUS_TYPE` varchar(32) NOT NULL DEFAULT 'BUILD' COMMENT '业务类型 BUILD:构建 INDEX:研发商店指标',
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
    `ID`          varchar(32)  NOT NULL COMMENT '主键ID',
    `TYPE`        varchar(16)  NOT NULL COMMENT '类型',
    `LOGO_URL`    varchar(512) NOT NULL DEFAULT '' COMMENT 'LOGO URL地址',
    `LINK`        varchar(512)          DEFAULT '' COMMENT '跳转链接',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER`    varchar(50)  NOT NULL DEFAULT 'system' COMMENT '修改者',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `ORDER`       int(8)       NOT NULL DEFAULT '0' COMMENT '显示顺序',
    PRIMARY KEY (`ID`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='商店logo信息表';

CREATE TABLE IF NOT EXISTS `T_REASON`
(
    `ID`          varchar(32)  NOT NULL COMMENT '主键',
    `TYPE`        varchar(32)  NOT NULL COMMENT '类型',
    `CONTENT`     varchar(512) NOT NULL DEFAULT '' COMMENT '日志内容',
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
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `TYPE`        varchar(32) NOT NULL COMMENT '类型',
    `STORE_CODE`  varchar(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
	`STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0' COMMENT 'store组件类型',
    `REASON_ID`   varchar(32) NOT NULL COMMENT '原因ID',
    `NOTE`        text COMMENT '原因说明',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `store_code` (`STORE_CODE`),
	KEY `inx_trr_store_type` (`STORE_TYPE`),
    KEY `type` (`TYPE`)
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
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `i_feature_business_businessValue` (`FEATURE`,`BUSINESS`,`BUSINESS_VALUE`) USING BTREE COMMENT '同一业务相同业务值的相同特性仅能有一个配置'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='store业务配置表';

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

CREATE TABLE IF NOT EXISTS `T_STORE_STATISTICS_DAILY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件编码',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '组件类型',
  `TOTAL_DOWNLOADS` int(11) DEFAULT '0' COMMENT '总下载量',
  `DAILY_DOWNLOADS` int(11) DEFAULT '0' COMMENT '每日下载量',
  `DAILY_SUCCESS_NUM` int(11) DEFAULT '0' COMMENT '每日执行成功数',
  `DAILY_FAIL_NUM` int(11) DEFAULT '0' COMMENT '每日执行失败总数',
  `DAILY_FAIL_DETAIL` varchar(4096) COMMENT '每日执行失败详情',
  `STATISTICS_TIME` datetime(3) NOT NULL  COMMENT '统计时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `UNI_INX_TSSD_CODE_TYPE_TIME` (`STORE_CODE`,`STORE_TYPE`, `STATISTICS_TIME`),
  KEY `INX_TSSD_STATISTICS_TIME` (`STATISTICS_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='store每日统计信息表';

CREATE TABLE IF NOT EXISTS  `T_STORE_SENSITIVE_API` (
  `ID` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'store组件编码',
  `STORE_TYPE` TINYINT(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  `API_NAME` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'API名称',
  `ALIAS_NAME` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '别名',
  `API_STATUS` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'API状态',
  `API_LEVEL` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'API等级',
  `APPLY_DESC` VARCHAR(1024) DEFAULT NULL COMMENT '申请说明',
  `APPROVE_MSG` VARCHAR(1024) DEFAULT NULL COMMENT '批准信息',
  `CREATOR` VARCHAR(50) NOT NULL COMMENT '创建者',
  `MODIFIER` VARCHAR(50) NOT NULL COMMENT '修改者',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQ_STORE_CODE_TYPE_NAME` (`STORE_CODE`,`STORE_TYPE`,`API_NAME`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='敏感API接口信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_DOCKING_PLATFORM` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `PLATFORM_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '平台代码',
  `PLATFORM_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '平台名称',
  `WEBSITE` varchar(256) DEFAULT '' COMMENT '网址',
  `SUMMARY` varchar(1024) NOT NULL DEFAULT '' COMMENT '简介',
  `PRINCIPAL` varchar(50) NOT NULL DEFAULT '' COMMENT '负责人',
  `LOGO_URL` varchar(256) DEFAULT '' COMMENT '平台logo地址',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `OWNER_DEPT_NAME` varchar(256) NOT NULL COMMENT '所属机构名称',
  `LABELS` varchar(1024) DEFAULT NULL COMMENT '标签',
  `ERROR_CODE_PREFIX` int(3) NULL COMMENT '平台所属错误码前缀',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TSDP_PLATFORM_CODE` (`PLATFORM_CODE`),
  UNIQUE KEY `UNI_INX_TSDP_PLATFORM_NAME` (`PLATFORM_NAME`),
  UNIQUE KEY `UNI_INX_TSDP_ERROR` (`ERROR_CODE_PREFIX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件对接平台信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_DOCKING_PLATFORM_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件编码',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '组件类型',
  `PLATFORM_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '平台代码',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TSDP_STORE_CODE_TYPE` (`STORE_CODE`,`STORE_TYPE`,`PLATFORM_CODE`),
  KEY `INX_TSDP_PLATFORM_CODE` (`PLATFORM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件对接平台关联关系表';

CREATE TABLE IF NOT EXISTS `T_STORE_PUBLISHER_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `PUBLISHER_CODE` varchar(64) NOT NULL COMMENT '发布者标识',
  `PUBLISHER_NAME` varchar(256) NOT NULL COMMENT '发布者名称',
  `PUBLISHER_TYPE` varchar(32) NOT NULL COMMENT '发布者类型 PERSON：个人，ORGANIZATION：组织',
  `OWNERS` varchar(1024) NOT NULL COMMENT '主体负责人',
  `HELPER` varchar(256) NOT NULL COMMENT '技术支持',
  `FIRST_LEVEL_DEPT_ID` bigint(20) NOT NULL COMMENT '一级部门ID',
  `FIRST_LEVEL_DEPT_NAME` varchar(256) NOT NULL COMMENT '一级部门名称',
  `SECOND_LEVEL_DEPT_ID` bigint(20) NOT NULL COMMENT '二级部门ID',
  `SECOND_LEVEL_DEPT_NAME` varchar(256) NOT NULL COMMENT '二级部门名称',
  `THIRD_LEVEL_DEPT_ID` bigint(20) NOT NULL COMMENT '三级部门ID',
  `THIRD_LEVEL_DEPT_NAME` varchar(256) NOT NULL COMMENT '三级部门名称',
  `FOURTH_LEVEL_DEPT_ID` bigint(20) DEFAULT NULL COMMENT '四级部门ID',
  `FOURTH_LEVEL_DEPT_NAME` varchar(256) DEFAULT NULL COMMENT '四级部门名称',
  `ORGANIZATION_NAME` varchar(256) NOT NULL COMMENT '组织架构名称',
  `BG_NAME` varchar(256) NOT NULL COMMENT '所属机构名称',
  `CERTIFICATION_FLAG` bit(1) DEFAULT b'0' COMMENT '是否认证标识 true：是，false：否',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TSPI_CODE_TYPE` (`PUBLISHER_CODE`,`STORE_TYPE`),
  UNIQUE KEY `UNI_INX_TSPI_NAME_TYPE` (`PUBLISHER_NAME`,`STORE_TYPE`),
  KEY `UNI_INX_TSPI_TYPE` (`PUBLISHER_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布者信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_PUBLISHER_MEMBER_REL` (
    `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `PUBLISHER_ID` varchar(32) NOT NULL COMMENT '发布者ID',
    `MEMBER_ID` varchar(32) NOT NULL COMMENT '成员ID',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `INX_TSPMR_PUBLISHER_ID` (`PUBLISHER_ID`),
    KEY `INX_TSPMR_MEMBER_ID` (`MEMBER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布者成员关联关系表';

CREATE TABLE IF NOT EXISTS `T_STORE_PKG_RUN_ENV_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '组件类型',
  `LANGUAGE` varchar(64) NOT NULL DEFAULT '' COMMENT '开发语言',
  `OS_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '支持的操作系统名称',
  `OS_ARCH` varchar(128) NOT NULL DEFAULT '' COMMENT '支持的操作系统架构',
  `RUNTIME_VERSION` varchar(128) NOT NULL DEFAULT '' COMMENT '运行时版本',
  `PKG_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '安装包名称',
  `PKG_DOWNLOAD_PATH` varchar(1024) NOT NULL DEFAULT '' COMMENT '安装包下载路径',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为默认安装包',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TSPREI_TYPE_OS_VERSION` (`STORE_TYPE`,`LANGUAGE`,`OS_NAME`,`OS_ARCH`,`RUNTIME_VERSION`),
  KEY `INX_TSPREI_RUNTIME_VERSION` (`RUNTIME_VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件安装包运行时环境信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_ERROR_CODE_INFO` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `STORE_CODE` varchar(64) DEFAULT NULL COMMENT '组件代码,为空则表示属于通用错误码',
  `ERROR_CODE` int(6) NOT NULL COMMENT '错误码',
  `STORE_TYPE` tinyint(4) DEFAULT NULL COMMENT '组件类型 0：插件 1：模板',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `UNI_TSECI_STORE_TYPE_ERROR` (`STORE_CODE`,`STORE_TYPE`,`ERROR_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store组件错误码信息';

CREATE TABLE IF NOT EXISTS  `T_STORE_HONOR_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `HONOR_TITLE` varchar(10) NOT NULL DEFAULT '' COMMENT '头衔',
  `HONOR_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '荣誉名称',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TSHI_TITLE` (`HONOR_TITLE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店荣誉信息表';

CREATE TABLE IF NOT EXISTS  `T_STORE_HONOR_REL` (
 `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
 `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件代码',
 `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
 `HONOR_ID` varchar(32) NOT NULL COMMENT '荣誉ID',
 `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
 `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
 `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
 `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
 `STORE_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '组件名称',
 `MOUNT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否佩戴',
 PRIMARY KEY (`ID`),
 KEY `INX_TSHR_TYPE_CODE` (`STORE_TYPE`,`STORE_CODE`),
 KEY `INX_TSHR_HONOR_ID` (`HONOR_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店荣誉关联信息表';

CREATE TABLE IF NOT EXISTS  `T_STORE_INDEX_BASE_INFO` (
    `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `INDEX_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '指标标识',
    `INDEX_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '指标名称',
    `DESCRIPTION` text COMMENT '描述',
    `OPERATION_TYPE` varchar(32) NOT NULL COMMENT '运算类型 ATOM:插件 PLATFORM:平台',
    `ATOM_CODE` varchar(64) DEFAULT NULL COMMENT '指标对应的插件件代码',
    `ATOM_VERSION` varchar(64) DEFAULT NULL COMMENT '指标对应的插件版本',
    `FINISH_TASK_NUM` int(11) DEFAULT NULL COMMENT '完成执行任务数量',
    `TOTAL_TASK_NUM` int(11) DEFAULT NULL COMMENT '执行任务总数',
    `EXECUTE_TIME_TYPE` varchar(32) NOT NULL COMMENT '执行时间类型 INDEX_CHANGE:指标变动 COMPONENT_UPGRADE:组件升级 CRON:定时',
    `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `WEIGHT` int(5) NOT NULL DEFAULT '0' COMMENT '指标展示权重',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_INX_TSIBI_TYPE_CODE` (`STORE_TYPE`,`INDEX_CODE`),
    UNIQUE KEY `UNI_INX_TSIBI_TYPE_NAME` (`STORE_TYPE`,`INDEX_NAME`),
    KEY `INX_TSIBI_TYPE_ATOM_TIME` (`STORE_TYPE`,`ATOM_CODE`,`EXECUTE_TIME_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店指标基本信息表';

CREATE TABLE IF NOT EXISTS  `T_STORE_INDEX_ELEMENT_DETAIL` (
    `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件代码',
    `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
    `INDEX_ID` varchar(32) NOT NULL COMMENT '指标ID',
    `INDEX_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '指标标识',
    `ELEMENT_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '指标要素名称',
    `ELEMENT_VALUE` varchar(64) NOT NULL DEFAULT '' COMMENT '指标要素值',
    `REMARK` varchar(1024) DEFAULT NULL COMMENT '备注',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `T_STORE_INDEX_ELEMENT_DETAIL_UN` (`STORE_TYPE`,`STORE_CODE`,`INDEX_CODE`,`ELEMENT_NAME`),
    KEY `INX_TSIR_INDEX_ID` (`INDEX_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店组件指标要素详情表';

CREATE TABLE IF NOT EXISTS  `T_STORE_INDEX_LEVEL_INFO` (
    `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `LEVEL_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '等级名称',
    `INDEX_ID` varchar(32) NOT NULL COMMENT '指标ID',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `ICON_URL` varchar(1024) NOT NULL DEFAULT '' COMMENT 'icon地址',
    PRIMARY KEY (`ID`),
    KEY `INX_TSILI_LEVEL_NAME` (`LEVEL_NAME`),
    KEY `INX_TSILI_INDEX_ID` (`INDEX_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店指标等级信息表';

CREATE TABLE IF NOT EXISTS  `T_STORE_INDEX_RESULT` (
    `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件代码',
    `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件 4：微扩展',
    `INDEX_ID` varchar(32) NOT NULL COMMENT '指标ID',
    `INDEX_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '指标标识',
    `ICON_TIPS` text NOT NULL COMMENT '图标提示信息',
    `LEVEL_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '等级ID',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `T_STORE_INDEX_RESULT_UN` (`STORE_TYPE`,`STORE_CODE`,`INDEX_CODE`),
    KEY `INX_TSIR_INDEX_ID` (`INDEX_ID`),
    KEY `INX_TSIR_LEVEL_ID` (`LEVEL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研发商店组件指标结果表';

CREATE TABLE IF NOT EXISTS  `T_STORE_DOCKING_PLATFORM_ERROR_CODE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `ERROR_CODE` int(6) NOT NULL COMMENT 'code码',
  `ERROR_MSG_ZH_CN` varchar(500) NOT NULL COMMENT '中文简体描述信息',
  `ERROR_MSG_ZH_TW` varchar(500) DEFAULT NULL COMMENT '中文繁体描述信息',
  `ERROR_MSG_EN` varchar(500) DEFAULT NULL COMMENT '英文描述信息',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `PLATFORM_CODE` varchar(64) CHARACTER SET utf8mb4 NOT NULL COMMENT '错误码所属平台代码',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_TSDPEC_PLATFORM_ERROR_CODE` (`PLATFORM_CODE`, `ERROR_CODE`),
  UNIQUE KEY `UNI_TSDPEC_ERROR_CODE` (`ERROR_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='平台所属错误码信息';

SET FOREIGN_KEY_CHECKS = 1;
