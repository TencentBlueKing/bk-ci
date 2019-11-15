USE devops_ci_measure;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_MEASURE_BUILD_ELEMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_BUILD_ELEMENT` (
  `elementName` varchar(64) NOT NULL,
  `pipelineId` varchar(34) NOT NULL,
  `buildId` varchar(34) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT '',
  `beginTime` datetime NOT NULL,
  `endTime` datetime NOT NULL,
  `projectId` varchar(32) NOT NULL,
  `extra` text,
  `type` varchar(32) NOT NULL,
  `elementId` varchar(64) NOT NULL DEFAULT '',
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `atomCode` varchar(128) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `index_buildId` (`buildId`),
  KEY `index_pipelineId` (`pipelineId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_MEASURE_DASHBOARD_VIEW
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_DASHBOARD_VIEW` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `projectId` varchar(36) NOT NULL,
  `user` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `viewConfig` mediumtext NOT NULL,
  `viewType` varchar(32) NOT NULL DEFAULT 'SINGLE',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_MEASURE_PIPELINE_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_PIPELINE_BUILD` (
  `pipelineId` varchar(34) NOT NULL,
  `buildId` varchar(34) NOT NULL,
  `beginTime` datetime NOT NULL,
  `endTime` datetime NOT NULL,
  `startType` varchar(20) NOT NULL,
  `buildUser` varchar(255) NOT NULL,
  `isParallel` bit(1) NOT NULL,
  `buildResult` varchar(20) NOT NULL,
  `projectId` varchar(32) NOT NULL,
  `pipeline` mediumtext NOT NULL,
  `buildNum` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `metaInfo` text,
  `parentPipelineId` varchar(34) DEFAULT NULL,
  `parentBuildId` varchar(34) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_pipelineId` (`pipelineId`),
  KEY `index_projectId` (`projectId`),
  KEY `inx_tmpb_begin_time` (`beginTime`),
  KEY `index_parentBuildId` (`parentBuildId`),
  KEY `index_parentPipelineId` (`parentPipelineId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_MEASURE_PROJECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_PROJECT` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `approval_status` int(11) DEFAULT NULL,
  `bg_id` int(11) DEFAULT NULL,
  `bg_name` varchar(120) DEFAULT NULL,
  `cc_app_id` int(11) DEFAULT NULL,
  `center_id` int(11) DEFAULT NULL,
  `center_name` varchar(120) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `creator` varchar(32) DEFAULT NULL,
  `data_id` int(11) DEFAULT NULL,
  `deploy_type` varchar(256) DEFAULT NULL,
  `dept_id` int(11) DEFAULT NULL,
  `dept_name` varchar(120) DEFAULT NULL,
  `description` text,
  `project_code` varchar(128) DEFAULT NULL,
  `is_offlined` tinyint(1) DEFAULT NULL,
  `is_secrecy` tinyint(1) DEFAULT NULL,
  `kind` int(11) DEFAULT NULL,
  `project_id` varchar(64) DEFAULT NULL,
  `project_name` varchar(256) DEFAULT NULL,
  `project_type` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `use_bk` tinyint(1) DEFAULT NULL,
  `logo_addr` varchar(1024) DEFAULT NULL,
  `pipeline_count` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `project_id` (`project_id`),
  KEY `inx_tmp_created_at` (`created_at`),
  KEY `inx_tmp_bg_name` (`bg_name`),
  KEY `inx_tmp_dept_name` (`dept_name`),
  KEY `inx_tmp_center_name` (`center_name`),
  KEY `inx_tmp_project_type` (`project_type`),
  KEY `inx_tmp_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_MEASURE_WETEST_INFO
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_MEASURE_WETEST_INFO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elementKeyId` int(11) NOT NULL,
  `testid` varchar(64) DEFAULT NULL,
  `passrate` int(11) DEFAULT '0',
  `failManuMap` text,
  `failVersionMap` text,
  `failResolutionMap` text,
  `errCodeMap` text,
  `errLevelMap` text,
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `elementKeyId_index` (`elementKeyId`),
  KEY `createtime_index` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
