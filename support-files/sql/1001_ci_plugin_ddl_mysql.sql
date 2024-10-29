USE devops_ci_plugin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_PLUGIN_GITHUB_CHECK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GITHUB_CHECK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `BUILD_NUMBER` int(11) NOT NULL COMMENT '构建编号',
  `REPO_ID` varchar(64) DEFAULT NULL COMMENT '代码库ID',
  `COMMIT_ID` varchar(64) NOT NULL COMMENT '代码提交ID',
  `CHECK_RUN_ID` bigint(20) NOT NULL COMMENT '',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPO_NAME` varchar(128) DEFAULT NULL COMMENT '代码库别名',
  `CHECK_RUN_NAME` VARCHAR(64) NULL DEFAULT NULL COMMENT '',
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID_REPO_ID_COMMIT_ID` (`PIPELINE_ID`,`COMMIT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_PLUGIN_GIT_CHECK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PLUGIN_GIT_CHECK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `BUILD_NUMBER` int(11) NOT NULL COMMENT '构建编号',
  `REPO_ID` varchar(64) DEFAULT NULL COMMENT '代码库ID',
  `COMMIT_ID` varchar(64) NOT NULL COMMENT '代码提交ID',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPO_NAME` varchar(128) DEFAULT NULL COMMENT '代码库别名',
  `CONTEXT` VARCHAR(255) DEFAULT NULL COMMENT '内容',
  `TARGET_BRANCH` VARCHAR(255) DEFAULT NULL COMMENT '目标分支',
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID_REPO_ID_COMMIT_ID` (`PIPELINE_ID`,`COMMIT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_AI_SCORE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AI_SCORE`
(
    `ID`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `LABEL`       varchar(256) NOT NULL COMMENT '任务ID',
    `ARCHIVE`     boolean      NOT NULL                             DEFAULT 0 COMMENT '是否已归档',
    `CREATE_TIME` datetime     NOT NULL                             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime     NOT NULL ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `GOOD_USERS`  text COMMENT '赞的人',
    `BAD_USERS`   text COMMENT '踩的人',
    `AI_MSG`      text COMMENT '大模型生成的内容',
    `SYSTEM_MSG`  text COMMENT 'Prompt for system',
    `USER_MSG`    text COMMENT 'Prompt for user',
    PRIMARY KEY (`ID`),
    INDEX IDX_LABEL (`LABEL`),
    INDEX IDX_CREATE_TIME (`CREATE_TIME`),
    INDEX IDX_ARCHIVE (`ARCHIVE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='脚本执行报错AI分析-评分';

SET FOREIGN_KEY_CHECKS = 1;
