USE devops_ci_measure;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_MEASURE_BUILD_ELEMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_BUILD_ELEMENT` (
  `elementName` varchar(64) NOT NULL COMMENT '元素名称',
  `pipelineId` varchar(34) NOT NULL COMMENT '流水线ID',
  `buildId` varchar(34) NOT NULL COMMENT '构建ID',
  `status` varchar(32) NOT NULL DEFAULT '' COMMENT '状态',
  `beginTime` datetime NOT NULL COMMENT '开始时间',
  `endTime` datetime NOT NULL COMMENT '结束时间',
  `projectId` varchar(32) NOT NULL COMMENT '项目ID',
  `extra` text COMMENT '额外信息',
  `type` varchar(32) NOT NULL COMMENT '类型',
  `elementId` varchar(64) NOT NULL DEFAULT '' COMMENT '插件 elementId',
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `atomCode` varchar(128) NOT NULL DEFAULT '' COMMENT '插件的唯一标识',
  PRIMARY KEY (`id`),
  KEY `index_buildId` (`buildId`),
  KEY `index_pipelineId` (`pipelineId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_MEASURE_DASHBOARD_VIEW
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_DASHBOARD_VIEW` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `projectId` varchar(36) NOT NULL COMMENT '项目ID',
  `user` varchar(32) NOT NULL COMMENT '用户',
  `NAME` varchar(64) NOT NULL COMMENT '名称',
  `viewConfig` mediumtext NOT NULL COMMENT '视图配置',
  `viewType` varchar(32) NOT NULL DEFAULT 'SINGLE' COMMENT '视图类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_MEASURE_PIPELINE_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_PIPELINE_BUILD` (
  `pipelineId` varchar(34) NOT NULL COMMENT '流水线ID',
  `buildId` varchar(34) NOT NULL COMMENT '构建ID',
  `beginTime` datetime NOT NULL COMMENT '流水线的启动时间',
  `endTime` datetime NOT NULL COMMENT '流水线的结束时间',
  `startType` varchar(20) NOT NULL COMMENT '流水线的启动方式',
  `buildUser` varchar(255) NOT NULL COMMENT '流水线的启动用户',
  `isParallel` bit(1) NOT NULL COMMENT '流水线的是否并行',
  `buildResult` varchar(20) NOT NULL COMMENT '流水线的构建结果',
  `projectId` varchar(32) NOT NULL COMMENT '项目ID',
  `pipeline` mediumtext NOT NULL COMMENT '流水线',
  `buildNum` int(11) NOT NULL COMMENT '构建版本号',
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `metaInfo` text COMMENT '元数据',
  `parentPipelineId` varchar(34) DEFAULT NULL COMMENT '启动子流水线的流水线ID',
  `parentBuildId` varchar(34) DEFAULT NULL COMMENT '启动子流水线的构建ID',
  PRIMARY KEY (`id`),
  KEY `index_pipelineId` (`pipelineId`),
  KEY `index_projectId` (`projectId`),
  KEY `inx_tmpb_begin_time` (`beginTime`),
  KEY `index_parentBuildId` (`parentBuildId`),
  KEY `index_parentPipelineId` (`parentPipelineId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_MEASURE_PROJECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_PROJECT` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_status` int(11) DEFAULT NULL COMMENT '审核状态',
  `bg_id` int(11) DEFAULT NULL COMMENT '事业群ID',
  `bg_name` varchar(120) DEFAULT NULL COMMENT '事业群名称',
  `cc_app_id` int(11) DEFAULT NULL COMMENT '应用ID',
  `center_id` int(11) DEFAULT NULL COMMENT '中心ID',
  `center_name` varchar(120) DEFAULT NULL COMMENT '中心名字',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `creator` varchar(32) DEFAULT NULL COMMENT '创建者',
  `data_id` int(11) DEFAULT NULL COMMENT '数据ID',
  `deploy_type` varchar(256) DEFAULT NULL COMMENT '部署类型',
  `dept_id` int(11) DEFAULT NULL COMMENT '项目所属二级机构ID',
  `dept_name` varchar(120) DEFAULT NULL COMMENT '项目所属二级机构名称',
  `description` text COMMENT '描述',
  `project_code` varchar(128) DEFAULT NULL COMMENT '用户组所属项目',
  `is_offlined` tinyint(1) DEFAULT NULL COMMENT '是否停用',
  `is_secrecy` tinyint(1) DEFAULT NULL COMMENT '是否保密',
  `kind` int(11) DEFAULT NULL COMMENT '容器类型',
  `project_id` varchar(64) DEFAULT NULL COMMENT '项目ID',
  `project_name` varchar(256) DEFAULT NULL COMMENT '项目名称',
  `project_type` int(11) DEFAULT NULL COMMENT '项目类型',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `use_bk` tinyint(1) DEFAULT NULL COMMENT '是否用蓝鲸',
  `logo_addr` varchar(1024) DEFAULT NULL COMMENT 'logo地址',
  `pipeline_count` int(11) DEFAULT '0' COMMENT '流水线数量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `project_id` (`project_id`),
  KEY `inx_tmp_created_at` (`created_at`),
  KEY `inx_tmp_bg_name` (`bg_name`),
  KEY `inx_tmp_dept_name` (`dept_name`),
  KEY `inx_tmp_center_name` (`center_name`),
  KEY `inx_tmp_project_type` (`project_type`),
  KEY `inx_tmp_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_MEASURE_WETEST_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_WETEST_INFO` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elementKeyId` int(11) NOT NULL COMMENT '元素Key id',
  `testid` varchar(64) DEFAULT NULL COMMENT '',
  `passrate` int(11) DEFAULT '0' COMMENT '通过率',
  `failManuMap` text COMMENT '',
  `failVersionMap` text COMMENT '失败版本map',
  `failResolutionMap` text COMMENT '失败解析map',
  `errCodeMap` text COMMENT '错误代码map',
  `errLevelMap` text COMMENT '错误等级map',
  `createTime` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `elementKeyId_index` (`elementKeyId`),
  KEY `createtime_index` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

SET FOREIGN_KEY_CHECKS = 1;
