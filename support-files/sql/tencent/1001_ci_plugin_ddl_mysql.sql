USE devops_ci_plugin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_JINGANG_META
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_JINGANG_META` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) DEFAULT NULL,
  `PIPELINE_ID` varchar(34) DEFAULT NULL,
  `NAME` varchar(32) DEFAULT NULL,
  `VALUE` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_ID` (`PROJECT_ID`,`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_CODECC
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_CODECC` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `PIPELINE_ID` varchar(34) DEFAULT NULL,
  `BUILD_ID` varchar(34) DEFAULT NULL,
  `TASK_ID` varchar(34) DEFAULT NULL,
  `TOOL_SNAPSHOT_LIST` longtext,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `BUILD_ID_KEY` (`BUILD_ID`),
  KEY `IDX_PIPELINE` (`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_CODECC_ELEMENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_CODECC_ELEMENT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(128) DEFAULT NULL,
  `PIPELINE_ID` varchar(34) DEFAULT NULL,
  `TASK_NAME` varchar(256) DEFAULT NULL,
  `TASK_CN_NAME` varchar(256) DEFAULT NULL,
  `TASK_ID` varchar(128) DEFAULT NULL,
  `IS_SYNC` varchar(6) DEFAULT NULL,
  `SCAN_TYPE` varchar(6) DEFAULT NULL,
  `LANGUAGE` varchar(1024) DEFAULT NULL,
  `PLATFORM` varchar(16) DEFAULT NULL,
  `TOOLS` varchar(1024) DEFAULT NULL,
  `PY_VERSION` varchar(16) DEFAULT NULL,
  `ESLINT_RC` varchar(16) DEFAULT NULL,
  `CODE_PATH` longtext,
  `SCRIPT_TYPE` varchar(16) DEFAULT NULL,
  `SCRIPT` longtext,
  `CHANNEL_CODE` varchar(16) DEFAULT NULL,
  `UPDATE_USER_ID` varchar(128) DEFAULT NULL,
  `IS_DELETE` varchar(6) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `SUB_PIPELINE_ID` varchar(34) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PROJECT_PIPELINE_INDEX` (`PROJECT_ID`,`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_GCLOUD_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GCLOUD_CONF` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `REGION` varchar(1024) DEFAULT NULL,
  `ADDRESS` varchar(1024) DEFAULT NULL,
  `ADDRESS_FILE` varchar(1024) DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `USER_ID` varchar(64) DEFAULT NULL,
  `REMARK` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_GCLOUD_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GIT_CHECK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PIPELINE_ID` varchar(64) NOT NULL,
  `BUILD_NUMBER` int(11) NOT NULL,
  `REPO_ID` varchar(64) DEFAULT NULL,
  `COMMIT_ID` varchar(64) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `REPO_NAME` varchar(128) DEFAULT NULL,
  `CONTEXT` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID_REPO_ID_COMMIT_ID` (`PIPELINE_ID`,`COMMIT_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2149726 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_GITHUB_CHECK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GITHUB_CHECK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PIPELINE_ID` varchar(64) NOT NULL,
  `BUILD_NUMBER` int(11) NOT NULL,
  `REPO_ID` varchar(64) DEFAULT NULL,
  `COMMIT_ID` varchar(64) NOT NULL,
  `CHECK_RUN_ID` int(11) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `REPO_NAME` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID_REPO_ID_COMMIT_ID` (`PIPELINE_ID`,`COMMIT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_GITHUB_DEV_STAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GITHUB_DEV_STAT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT 'ID',
  `OWNER` varchar(64) NOT NULL DEFAULT '' COMMENT '代码库owner',
  `REPO` varchar(64) NOT NULL DEFAULT '' COMMENT '代码库英文名',
  `STAT_DATE` varchar(32) NOT NULL DEFAULT '' COMMENT '统计日期',
  `AUTHOR` varchar(64) NOT NULL DEFAULT '' COMMENT '用户名',
  `COMMITS` int(11) NOT NULL DEFAULT '0' COMMENT '提交数',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PER_REPO` (`OWNER`,`REPO`,`STAT_DATE`,`AUTHOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='代码库下的开发人员相关数据统计';

-- ----------------------------
-- Table structure for T_PLUGIN_GITHUB_STAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GITHUB_STAT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT 'ID',
  `OWNER` varchar(64) NOT NULL DEFAULT '' COMMENT '代码库owner',
  `REPO` varchar(64) NOT NULL DEFAULT '' COMMENT '代码库英文名',
  `STAT_DATE` varchar(32) NOT NULL DEFAULT '' COMMENT '统计日期',
  `ISSUE_CNT_ALL` int(11) DEFAULT '0' COMMENT 'issue总数',
  `ISSUE_CNT_OPEN` int(11) DEFAULT '0' COMMENT 'open的issue数',
  `ISSUE_CNT_CLOSED` int(11) DEFAULT '0' COMMENT '已关闭的issue数',
  `PR_CNT_ALL` int(11) DEFAULT '0' COMMENT 'pr总数',
  `PR_CNT_OPEN` int(11) DEFAULT '0' COMMENT 'open的pr数',
  `PR_CNT_CLOSED` int(11) DEFAULT '0' COMMENT '关闭的pr数',
  `COMMITS_CNT` int(11) DEFAULT '0' COMMENT '提交总数',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PER_REPO` (`OWNER`,`REPO`,`STAT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='代码库相关数据统计';

-- ----------------------------
-- Table structure for T_PLUGIN_JINGANG
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_JINGANG` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) DEFAULT NULL,
  `PIPELINE_ID` varchar(34) DEFAULT NULL,
  `BUILD_ID` varchar(34) DEFAULT NULL,
  `BUILD_NO` int(11) DEFAULT NULL,
  `USER_ID` varchar(64) DEFAULT NULL,
  `FILE_PATH` varchar(4096) DEFAULT NULL,
  `FILE_MD5` varchar(32) DEFAULT NULL,
  `FILE_SIZE` bigint(20) DEFAULT NULL,
  `VERSION` varchar(256) DEFAULT NULL COMMENT '版本号',
  `CREATE_TIME` datetime DEFAULT NULL,
  `UPDATE_TIME` datetime DEFAULT NULL,
  `STATUS` int(11) DEFAULT NULL COMMENT '(0:成功，其他失败，没有则是执行中)',
  `TYPE` int(6) DEFAULT NULL COMMENT '(0:android,1:ios)',
  `SCAN_URL` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `BUILD_ID` (`BUILD_ID`,`FILE_MD5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_JINGANG_RESULT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_JINGANG_RESULT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BUILD_ID` varchar(34) DEFAULT NULL,
  `FILE_MD5` varchar(64) DEFAULT NULL,
  `RESULT` mediumtext,
  `TASK_ID` bigint(20) DEFAULT NULL COMMENT '对应JINGANG表的id',
  PRIMARY KEY (`ID`),
  KEY `BUILD_ID` (`BUILD_ID`,`FILE_MD5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_MOOC
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_MOOC` (
  `ID` varchar(32) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'ID',
  `USER_ID` varchar(64) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '用户名',
  `COURSE_ID` varchar(64) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT '课程ID',
  `PROPS` text COLLATE utf8_bin COMMENT '参数JSON串',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_course_user` (`USER_ID`,`COURSE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='Mooc课程学习信息表';

-- ----------------------------
-- Table structure for T_PLUGIN_TASK_DATA
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_TASK_DATA` (
  `PROJECT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `BUILD_ID` varchar(34) NOT NULL,
  `VM_SEQ_ID` varchar(255) NOT NULL,
  `AGENT_ID` varchar(32) NOT NULL,
  `SECRET_KEY` varchar(32) NOT NULL,
  `CREATE_TIME` datetime NOT NULL,
  `UPDATE_TIME` datetime NOT NULL,
  PRIMARY KEY (`PROJECT_ID`,`PIPELINE_ID`,`BUILD_ID`,`VM_SEQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_TGIT_GROUP_STAT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_TGIT_GROUP_STAT` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
  `GROUP` varchar(64) NOT NULL DEFAULT '' COMMENT 'GIT项目组路径',
  `STAT_DATE` date NOT NULL COMMENT '日期',
  `PROJECT_COUNT` int(11) DEFAULT '0' COMMENT '项目总数',
  `PROJECT_COUNT_OPEN` int(11) DEFAULT '0' COMMENT '开源项目总数',
  `PROJECT_INCRE` int(11) DEFAULT '0' COMMENT '项目数增量',
  `PROJECT_INCRE_OPEN` int(11) DEFAULT '0' COMMENT '开源项目数增量',
  `COMMIT_COUNT` int(11) DEFAULT '0' COMMENT '提交总数',
  `COMMIT_COUNT_OPEN` int(11) DEFAULT '0' COMMENT '开源项目提交总数',
  `COMMIT_INCRE` int(11) DEFAULT '0' COMMENT '提交数增量',
  `COMMIT_INCRE_OPEN` int(11) DEFAULT '0' COMMENT '开源项目提交增量',
  `USER_COUNT` int(11) DEFAULT '0' COMMENT '提交用户数',
  `USER_COUNT_OPEN` int(11) DEFAULT '0' COMMENT '开源项目提交用户数',
  `USER_INCRE` int(11) DEFAULT '0' COMMENT '提交用户增量',
  `USER_INCRE_OPEN` int(11) DEFAULT '0' COMMENT '开源项目提交用户增量',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `GROUP` (`GROUP`,`STAT_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='插件代码库统计信息表';

-- ----------------------------
-- Table structure for T_PLUGIN_THIRDPARTY_AGENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_THIRDPARTY_AGENT` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) NOT NULL,
  `HOSTNAME` varchar(128) DEFAULT '',
  `IP` varchar(64) DEFAULT '',
  `OS` varchar(16) NOT NULL,
  `DETECT_OS` varchar(128) DEFAULT '',
  `STATUS` int(11) NOT NULL,
  `SECRET_KEY` varchar(256) NOT NULL,
  `CREATED_USER` varchar(64) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_WETEST_EAMIL_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_WETEST_EAMIL_GROUP` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(128) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `USER_INTERNAL` longtext,
  `QQ_EXTERNAL` longtext,
  `DESCRIPTION` varchar(1024) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `WETEST_GROUP_ID` varchar(128) DEFAULT NULL,
  `WETEST_GROUP_NAME` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_WETEST_INST_RESULT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_WETEST_INST_RESULT` (
  `ID` int(11) NOT NULL,
  `TEST_ID` varchar(64) DEFAULT NULL,
  `RESULT` longtext,
  `FINISH_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_WETEST_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_WETEST_TASK` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(128) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `MOBILE_CATEGORY` varchar(64) NOT NULL,
  `MOBILE_CATEGORY_ID` varchar(64) NOT NULL,
  `MOBILE_MODEL` longtext NOT NULL,
  `MOBILE_MODEL_ID` longtext,
  `DESCRIPTION` varchar(1024) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TICKETS_ID` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=12578 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_WETEST_TASK_INST
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_WETEST_TASK_INST` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `TEST_ID` varchar(64) NOT NULL,
  `PROJECT_ID` varchar(128) NOT NULL,
  `PIPELINE_ID` varchar(64) NOT NULL,
  `BUILD_ID` varchar(64) NOT NULL,
  `BUILD_NO` int(11) DEFAULT NULL,
  `NAME` varchar(2048) DEFAULT NULL,
  `VERSION` varchar(1024) DEFAULT NULL,
  `PASSING_RATE` varchar(16) DEFAULT NULL,
  `TASK_ID` int(11) DEFAULT NULL,
  `TEST_TYPE` varchar(64) DEFAULT NULL,
  `SCRIPT_TYPE` varchar(64) DEFAULT NULL,
  `IS_SYNC` varchar(4) DEFAULT NULL,
  `SOURCE_TYPE` varchar(12) DEFAULT NULL,
  `SOURCE_PATH` longtext,
  `ACCOUNT_PATH_TYPE` varchar(12) DEFAULT NULL,
  `ACCOUNT_PATH` longtext,
  `SCRIPT_PATH_TYPE` varchar(12) DEFAULT NULL,
  `SCRIPT_PATH` longtext,
  `TICKET_ID` varchar(64) DEFAULT NULL,
  `IS_PRIVATE_CLOUD` varchar(4) DEFAULT NULL,
  `START_USER` varchar(128) DEFAULT NULL,
  `BEGIN_TIME` datetime DEFAULT NULL,
  `END_TIME` datetime DEFAULT NULL,
  `STATUS` varchar(8) DEFAULT NULL COMMENT 'RUNNING,FAIL,SUCCESS',
  `EMAIL_GROUP_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `TEST_ID_INDEX` (`TEST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_PLUGIN_ZHIYUN_PRODUCT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_ZHIYUN_PRODUCT` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PRODUCT_ID` varchar(128) NOT NULL,
  `PRODUCT_NAME` varchar(128) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PRODUCT_ID` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
