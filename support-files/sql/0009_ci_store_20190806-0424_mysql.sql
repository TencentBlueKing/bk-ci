USE devops_ci_store;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APPS
-- ----------------------------
DROP TABLE IF EXISTS `T_APPS`;
CREATE TABLE `T_APPS` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '编译环境名称',
  `OS` varchar(32) NOT NULL DEFAULT '' COMMENT '操作系统',
  `BIN_PATH` varchar(64) DEFAULT NULL COMMENT '运行路径',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_OS` (`NAME`,`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='编译环境信息表';

-- ----------------------------
-- Table structure for T_APP_ENV
-- ----------------------------
DROP TABLE IF EXISTS `T_APP_ENV`;
CREATE TABLE `T_APP_ENV` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `APP_ID` int(11) NOT NULL COMMENT '编译环境ID',
  `PATH` varchar(32) NOT NULL DEFAULT '' COMMENT '路径',
  `NAME` varchar(32) NOT NULL DEFAULT '' COMMENT '名称',
  `DESCRIPTION` varchar(64) NOT NULL DEFAULT '' COMMENT '描述',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='编译环境变量表';

-- ----------------------------
-- Table structure for T_APP_VERSION
-- ----------------------------
DROP TABLE IF EXISTS `T_APP_VERSION`;
CREATE TABLE `T_APP_VERSION` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `APP_ID` int(11) NOT NULL COMMENT '编译环境ID',
  `VERSION` varchar(32) DEFAULT '' COMMENT '版本号',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `APP_ID` (`APP_ID`,`VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='编译环境版本信息表';

-- ----------------------------
-- Table structure for T_ATOM
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM`;
CREATE TABLE `T_ATOM` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `NAME` varchar(64) NOT NULL COMMENT '原子名称',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '原子代码',
  `CLASS_TYPE` varchar(64) NOT NULL COMMENT '原子大类（原子市场发布的原子分为有marketBuild：构建环境和marketBuildLess：无构建环境）',
  `SERVICE_SCOPE` varchar(256) NOT NULL COMMENT '服务范围（以JSON串的方式存进去，比如一个原子同时可在流水线和质量红线两个服务中被使用，则存为 ["PIPELINE","QUALITY"]）',
  `JOB_TYPE` varchar(20) DEFAULT NULL COMMENT 'Job类型， AGENT： 编译环境，AGENT_LESS：无编译环境',
  `OS` varchar(100) NOT NULL COMMENT '支持的操作系统（比如一个原子同时可在WINDOWS和LINUX和MACOS下使用，则为["WINDOWS","LINUX","MACOS"]）',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `DOCS_LINK` varchar(256) DEFAULT NULL COMMENT '原子说明文档链接',
  `ATOM_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '原子类型，0：自研 1：第三方开发',
  `ATOM_STATUS` tinyint(4) NOT NULL COMMENT '原子状态，0：初始化|1：提交中|2：构建中|3：构建失败|4：测试中|5：审核中|6：审核驳回|7：已发布|8：上架中止|9：下架中|10：已下架',
  `ATOM_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT '原子简介',
  `DESCRIPTION` text COMMENT '原子描述',
  `CATEGROY` tinyint(4) NOT NULL DEFAULT '1' COMMENT '所属范畴，0：触发器类原子 1：任务类原子',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `ICON` text COMMENT '原子图标(BASE64字符串)',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为默认原子（默认原子默认所有项目可见），TRUE：默认原子 FALSE：普通原子',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本原子， TRUE：最新 FALSE：非最新',
  `BUILD_LESS_RUN_FLAG` bit(1) DEFAULT NULL COMMENT '无构建环境原子是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以',
  `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '代码库hashId',
  `CODE_SRC` varchar(256) DEFAULT NULL COMMENT '代码库地址',
  `PAY_FLAG` bit(1) DEFAULT b'1' COMMENT '是否免费， TRUE：免费 FALSE：收费',
  `HTML_TEMPLATE_VERSION` varchar(10) NOT NULL DEFAULT '1.1' COMMENT '前端渲染模板版本（1.0代表历史存量原子渲染模板版本）',
  `PROPS` text COMMENT '自定义扩展容器前端表单属性字段的JSON串',
  `DATA` text COMMENT '预留字段（设置规则等信息的JSON串）',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '原子发布者',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0' COMMENT '原子插件代码库可见范围 0：私有 10：登录用户开源',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子表';

-- ----------------------------
-- Table structure for T_ATOM_BUILD_APP_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_BUILD_APP_REL`;
CREATE TABLE `T_ATOM_BUILD_APP_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `BUILD_INFO_ID` varchar(32) NOT NULL COMMENT '构建信息Id(对应T_ATOM_BUILD_INFO主键)',
  `APP_VERSION_ID` int(11) DEFAULT NULL COMMENT '编译环境版本Id(对应T_APP_VERSION主键)',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tabar_build_info_id` (`BUILD_INFO_ID`),
  KEY `inx_tabar_app_version_id` (`APP_VERSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子构建与编译环境关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_BUILD_INFO
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_BUILD_INFO`;
CREATE TABLE `T_ATOM_BUILD_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `LANGUAGE` varchar(64) DEFAULT NULL COMMENT '原子开发语言',
  `SCRIPT` text NOT NULL COMMENT '打包脚本',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPOSITORY_PATH` varchar(500) DEFAULT NULL COMMENT 'ä»£ç å­˜æ”¾è·¯å¾„',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tabi_language` (`LANGUAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子构建信息表';

-- ----------------------------
-- Table structure for T_ATOM_ENV_INFO
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_ENV_INFO`;
CREATE TABLE `T_ATOM_ENV_INFO` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '原子ID',
  `PKG_PATH` varchar(1024) NOT NULL COMMENT '安装包路径',
  `LANGUAGE` varchar(64) DEFAULT NULL COMMENT '原子开发语言',
  `MIN_VERSION` varchar(20) DEFAULT NULL COMMENT '支持原子开发语言的最低版本',
  `TARGET` varchar(256) NOT NULL COMMENT '原子执行入口',
  `SHA_CONTENT` varchar(1024) DEFAULT NULL COMMENT '原子SHA签名串',
  `PRE_CMD` text COMMENT '原子执行前置命令',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `PKG_NAME` varchar(256) DEFAULT '' COMMENT '插件包名',
  PRIMARY KEY (`ID`),
  KEY `inx_tpaei_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子执行环境信息表';

-- ----------------------------
-- Table structure for T_ATOM_FEATURE
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_FEATURE`;
CREATE TABLE `T_ATOM_FEATURE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '原子插件代码',
  `VISIBILITY_LEVEL` int(11) NOT NULL DEFAULT '0' COMMENT '原子插件代码库可见范围 0：私有 10：登录用户开源',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_taf_code` (`ATOM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='原子插件特性信息表';

-- ----------------------------
-- Table structure for T_ATOM_LABEL_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_LABEL_REL`;
CREATE TABLE `T_ATOM_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '原子标签ID',
  `ATOM_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '原子ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_talr_label_id` (`LABEL_ID`),
  KEY `inx_talr_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='原子与标签关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_OFFLINE
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_OFFLINE`;
CREATE TABLE `T_ATOM_OFFLINE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '原子代码',
  `BUFFER_DAY` tinyint(4) NOT NULL COMMENT '缓冲期天数',
  `EXPIRE_TIME` datetime NOT NULL COMMENT '到期时间',
  `STATUS` tinyint(4) NOT NULL COMMENT '状态：0 未完成  1 已完成',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tao_atom_code` (`ATOM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='原子下架表';

-- ----------------------------
-- Table structure for T_ATOM_OPERATE_LOG
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_OPERATE_LOG`;
CREATE TABLE `T_ATOM_OPERATE_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '原子ID',
  `CONTENT` text NOT NULL COMMENT '操作日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tpaol_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子操作日志表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_BUILD_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_PIPELINE_BUILD_REL`;
CREATE TABLE `T_ATOM_PIPELINE_BUILD_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '原子ID',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tapbr_atom_id` (`ATOM_ID`),
  KEY `inx_tapbr_pipeline_id` (`PIPELINE_ID`),
  KEY `inx_tapbr_build_id` (`BUILD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子构建关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_PIPELINE_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_PIPELINE_REL`;
CREATE TABLE `T_ATOM_PIPELINE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_CODE` varchar(64) NOT NULL COMMENT '原子代码',
  `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `inx_tapr_atom_code` (`ATOM_CODE`),
  KEY `inx_tapr_pipeline_id` (`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子与流水线关联关系表';

-- ----------------------------
-- Table structure for T_ATOM_VERSION_LOG
-- ----------------------------
DROP TABLE IF EXISTS `T_ATOM_VERSION_LOG`;
CREATE TABLE `T_ATOM_VERSION_LOG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `ATOM_ID` varchar(32) NOT NULL COMMENT '原子ID',
  `RELEASE_TYPE` tinyint(4) NOT NULL COMMENT '发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正',
  `CONTENT` text NOT NULL COMMENT '版本日志内容',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tpavl_atom_id` (`ATOM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子版本日志表';

-- ----------------------------
-- Table structure for T_BUILD_RESOURCE
-- ----------------------------
DROP TABLE IF EXISTS `T_BUILD_RESOURCE`;
CREATE TABLE `T_BUILD_RESOURCE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `BUILD_RESOURCE_CODE` varchar(30) NOT NULL COMMENT '构建资源代码',
  `BUILD_RESOURCE_NAME` varchar(45) NOT NULL COMMENT '构建资源名称',
  `DEFAULT_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为默认构建资源，TRUE：是 FALSE：否',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpbr_code` (`BUILD_RESOURCE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线构建资源信息表';

-- ----------------------------
-- Table structure for T_CATEGORY
-- ----------------------------
DROP TABLE IF EXISTS `T_CATEGORY`;
CREATE TABLE `T_CATEGORY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `CATEGORY_CODE` varchar(32) NOT NULL COMMENT '范畴代码',
  `CATEGORY_NAME` varchar(32) NOT NULL COMMENT '范畴名称',
  `ICON_URL` varchar(256) DEFAULT NULL COMMENT 'icon地址',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '范畴类型，0：插件 1：模板',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tc_name_type` (`CATEGORY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_tc_code_code` (`CATEGORY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='范畴信息表';

-- ----------------------------
-- Table structure for T_CLASSIFY
-- ----------------------------
DROP TABLE IF EXISTS `T_CLASSIFY`;
CREATE TABLE `T_CLASSIFY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `CLASSIFY_CODE` varchar(32) NOT NULL COMMENT '分类代码',
  `CLASSIFY_NAME` varchar(32) NOT NULL COMMENT '分类名称',
  `WEIGHT` int(11) DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分类类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`CLASSIFY_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`CLASSIFY_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线原子分类信息表';

-- ----------------------------
-- Table structure for T_CONTAINER
-- ----------------------------
DROP TABLE IF EXISTS `T_CONTAINER`;
CREATE TABLE `T_CONTAINER` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `NAME` varchar(45) NOT NULL COMMENT '构建容器名称，唯一',
  `TYPE` varchar(20) NOT NULL COMMENT '流水线容器类型，有3个枚举值可选：trigger/vmBuild/normal 分别是：触发器/构建环境/无编译环境',
  `OS` varchar(15) NOT NULL COMMENT '操作系统，有4个枚举值可选： WINDOWS/MACOS/LINUX/NONE',
  `REQUIRED` tinyint(4) NOT NULL DEFAULT '0' COMMENT '容器是否为必需，0:非必需 1：必需（如trigger容器必需）',
  `MAX_QUEUE_MINUTES` int(11) DEFAULT '60' COMMENT '最长排队时间，仅在vmBuid类型有用',
  `MAX_RUNNING_MINUTES` int(11) DEFAULT '600' COMMENT '最长运行时间，仅在vmBuid类型有用',
  `PROPS` text COMMENT '自定义扩展容器前端表单属性字段的Json串',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tpc_name` (`NAME`),
  KEY `inx_tpc_os` (`OS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线构建容器表（这里的容器与Docker不是同一个概念，而是流水线模型中的一个元素）';

-- ----------------------------
-- Table structure for T_CONTAINER_RESOURCE_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_CONTAINER_RESOURCE_REL`;
CREATE TABLE `T_CONTAINER_RESOURCE_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `CONTAINER_ID` varchar(32) NOT NULL COMMENT '容器ID',
  `RESOURCE_ID` varchar(32) NOT NULL COMMENT '构建资源ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tpcrr_container_id` (`CONTAINER_ID`),
  KEY `inx_tpcrr_resource_id` (`RESOURCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流水线容器与构建资源关联关系表';

-- ----------------------------
-- Table structure for T_LABEL
-- ----------------------------
DROP TABLE IF EXISTS `T_LABEL`;
CREATE TABLE `T_LABEL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `LABEL_CODE` varchar(32) NOT NULL COMMENT '标签代码',
  `LABEL_NAME` varchar(32) NOT NULL COMMENT '标签名称',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '标签类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_name_type` (`LABEL_NAME`,`TYPE`),
  UNIQUE KEY `uni_inx_code_type` (`LABEL_CODE`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='原子标签信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_COMMENT`;
CREATE TABLE `T_STORE_COMMENT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_ID` varchar(32) NOT NULL COMMENT 'store组件ID',
  `STORE_CODE` varchar(64) NOT NULL COMMENT 'store组件代码',
  `COMMENT_CONTENT` text NOT NULL COMMENT '评论回复内容',
  `COMMENTER_DEPT` varchar(200) NOT NULL COMMENT '评论者组织架构',
  `SCORE` int(11) NOT NULL COMMENT '评分',
  `PRAISE_COUNT` int(11) DEFAULT '0' COMMENT '点赞个数',
  `PROFILE_URL` varchar(256) DEFAULT NULL COMMENT '评论者头像url地址',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tsc_id` (`STORE_ID`),
  KEY `inx_tsc_code` (`STORE_CODE`),
  KEY `inx_tsc_type` (`STORE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store组件评论信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_PRAISE
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_COMMENT_PRAISE`;
CREATE TABLE `T_STORE_COMMENT_PRAISE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `COMMENT_ID` varchar(32) NOT NULL COMMENT '评论ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人（点赞人）',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tscp_id_creator` (`COMMENT_ID`,`CREATOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store组件评论点赞信息表';

-- ----------------------------
-- Table structure for T_STORE_COMMENT_REPLY
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_COMMENT_REPLY`;
CREATE TABLE `T_STORE_COMMENT_REPLY` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `COMMENT_ID` varchar(32) NOT NULL COMMENT '评论ID',
  `REPLY_CONTENT` text COMMENT '评论回复内容',
  `PROFILE_URL` varchar(256) DEFAULT NULL COMMENT '评论回复者头像url地址',
  `REPLY_TO_USER` varchar(50) DEFAULT NULL COMMENT '被回复者',
  `REPLYER_DEPT` varchar(200) NOT NULL COMMENT '评论回复者组织架构',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tscr_comment_id` (`COMMENT_ID`),
  KEY `inx_tscr_user` (`REPLY_TO_USER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store组件评论回复信息表';

-- ----------------------------
-- Table structure for T_STORE_DEPT_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_DEPT_REL`;
CREATE TABLE `T_STORE_DEPT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
  `DEPT_ID` int(11) NOT NULL COMMENT '机构ID',
  `DEPT_NAME` varchar(1024) NOT NULL COMMENT '机构名称',
  `STATUS` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态，0：待审核 1：审核通过 2：审核驳回',
  `COMMENT` varchar(256) DEFAULT NULL COMMENT '批注',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  KEY `inx_tpcadr_dept_id` (`DEPT_ID`),
  KEY `inx_tsdr_code` (`STORE_CODE`),
  KEY `inx_tsdr_type` (`STORE_TYPE`),
  KEY `inx_tsdr_status` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商店组件与与机构关联关系表';

-- ----------------------------
-- Table structure for T_STORE_MEMBER
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_MEMBER`;
CREATE TABLE `T_STORE_MEMBER` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '组件编码',
  `USERNAME` varchar(64) NOT NULL COMMENT '成员名称',
  `TYPE` tinyint(4) NOT NULL COMMENT '成员类型，0：管理员 1：开发人员',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '组件类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tam_code_name_type` (`STORE_CODE`,`USERNAME`,`STORE_TYPE`),
  KEY `inx_tam_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store组件成员信息表';

-- ----------------------------
-- Table structure for T_STORE_PROJECT_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_PROJECT_REL`;
CREATE TABLE `T_STORE_PROJECT_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
  `PROJECT_CODE` varchar(32) NOT NULL COMMENT '项目编码',
  `TYPE` tinyint(4) NOT NULL COMMENT '类型 0:新增原子时关联的项目 1：安装原子时关联的项目',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tspr_code_type` (`STORE_CODE`,`PROJECT_CODE`,`STORE_TYPE`),
  KEY `inx_tpapr_project_code` (`PROJECT_CODE`),
  KEY `inx_tspr_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商店组件与项目关联关系表';

-- ----------------------------
-- Table structure for T_STORE_SENSITIVE_CONF
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_SENSITIVE_CONF`;
CREATE TABLE `T_STORE_SENSITIVE_CONF` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_STORE_STATISTICS
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_STATISTICS`;
CREATE TABLE `T_STORE_STATISTICS` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '商店组件ID',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
  `DOWNLOADS` int(11) DEFAULT NULL COMMENT '总下载量',
  `COMMITS` int(11) DEFAULT NULL COMMENT '总评论量',
  `SCORE` int(11) DEFAULT NULL COMMENT '总评分',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tss_id_code_type` (`STORE_ID`,`STORE_CODE`,`STORE_TYPE`),
  KEY `ATOM_CODE` (`STORE_CODE`),
  KEY `inx_tss_id` (`STORE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store统计信息表';

-- ----------------------------
-- Table structure for T_STORE_STATISTICS_TOTAL
-- ----------------------------
DROP TABLE IF EXISTS `T_STORE_STATISTICS_TOTAL`;
CREATE TABLE `T_STORE_STATISTICS_TOTAL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `STORE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
  `STORE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板',
  `DOWNLOADS` int(11) DEFAULT '0' COMMENT '总下载量',
  `COMMITS` int(11) DEFAULT '0' COMMENT '总评论量',
  `SCORE` int(11) DEFAULT '0' COMMENT '总评分',
  `SCORE_AVERAGE` decimal(3,1) DEFAULT '0.0' COMMENT '平均评分',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tss_code_type` (`STORE_CODE`,`STORE_TYPE`),
  KEY `inx_tss_downloads` (`DOWNLOADS`),
  KEY `inx_tss_comments` (`COMMITS`),
  KEY `inx_tss_score` (`SCORE`),
  KEY `inx_tss_scoreAverage` (`SCORE_AVERAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='store全量统计信息表';

-- ----------------------------
-- Table structure for T_TEMPLATE
-- ----------------------------
DROP TABLE IF EXISTS `T_TEMPLATE`;
CREATE TABLE `T_TEMPLATE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `TEMPLATE_NAME` varchar(200) NOT NULL COMMENT '模板名称',
  `TEMPLATE_CODE` varchar(32) NOT NULL COMMENT '模板代码（对应process数据库T_TEMPLATE的ID）',
  `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '所属分类ID',
  `VERSION` varchar(20) NOT NULL COMMENT '版本号',
  `TEMPLATE_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '模板类型，0：自由模式 1：约束模式',
  `TEMPLATE_RD_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '模板研发类型，0：自研 1：第三方开发',
  `TEMPLATE_STATUS` tinyint(4) NOT NULL COMMENT '模板状态，0：初始化|1：审核中|2：审核驳回|3：已发布|4：上架中止|5：已下架',
  `TEMPLATE_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
  `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `SUMMARY` varchar(256) DEFAULT NULL COMMENT '模板简介',
  `DESCRIPTION` text COMMENT '模板描述',
  `PUBLISHER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '模板发布者',
  `PUB_DESCRIPTION` text COMMENT '发布描述',
  `PUBLIC_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为公共模板， TRUE：是 FALSE：不是',
  `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否为最新版本原子， TRUE：最新 FALSE：非最新',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tt_code_version` (`TEMPLATE_CODE`,`VERSION`),
  KEY `inx_tt_template_name` (`TEMPLATE_NAME`),
  KEY `inx_tt_template_code` (`TEMPLATE_CODE`),
  KEY `inx_tt_template_type` (`TEMPLATE_TYPE`),
  KEY `inx_tt_template_rd_type` (`TEMPLATE_RD_TYPE`),
  KEY `inx_tt_status` (`TEMPLATE_STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='模板信息表';

-- ----------------------------
-- Table structure for T_TEMPLATE_CATEGORY_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_TEMPLATE_CATEGORY_REL`;
CREATE TABLE `T_TEMPLATE_CATEGORY_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `CATEGORY_ID` varchar(32) NOT NULL COMMENT '模板范畴ID',
  `TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_ttcr_category_id` (`CATEGORY_ID`),
  KEY `inx_ttcr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='模板与范畴关联关系表';

-- ----------------------------
-- Table structure for T_TEMPLATE_LABEL_REL
-- ----------------------------
DROP TABLE IF EXISTS `T_TEMPLATE_LABEL_REL`;
CREATE TABLE `T_TEMPLATE_LABEL_REL` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `LABEL_ID` varchar(32) NOT NULL COMMENT '模板标签ID',
  `TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `inx_tstlr_label_id` (`LABEL_ID`),
  KEY `inx_tstlr_template_id` (`TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='模板与标签关联关系表';

SET FOREIGN_KEY_CHECKS = 1;
