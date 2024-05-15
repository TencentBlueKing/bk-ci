USE devops_ci_log;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_LOG_INDICES_V2
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LOG_INDICES_V2`
(
    `ID`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `BUILD_ID`      varchar(64) NOT NULL COMMENT '构建ID',
    `INDEX_NAME`    varchar(20) NOT NULL COMMENT '',
    `LAST_LINE_NUM` bigint(20)  NOT NULL DEFAULT '1' COMMENT '最后行号',
    `CREATED_TIME`  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATED_TIME`  timestamp   NOT NULL DEFAULT '2019-11-11 00:00:00' COMMENT '修改时间',
    `ENABLE`        bit(1)      NOT NULL DEFAULT b'0' COMMENT 'build is enable v2 or not',
    `LOG_CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT 'multi es log cluster name',
    `USE_CLUSTER` bit(1) NOT NULL DEFAULT b'0' COMMENT 'use multi es log cluster or not',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `BUILD_ID` (`BUILD_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='构建日志已关联ES索引表';

-- ----------------------------
-- Table structure for T_LOG_STATUS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LOG_STATUS`
(
    `ID`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `BUILD_ID`      varchar(64) NOT NULL COMMENT '构建ID',
    `TAG`           varchar(64)          DEFAULT NULL COMMENT '标签',
    `SUB_TAG` varchar(256) DEFAULT NULL COMMENT '子标签',
    `JOB_ID`        varchar(64)          DEFAULT NULL COMMENT 'JOB ID',
	`USER_JOB_ID` varchar(128) NULL COMMENT '真正的jobId，已经存在的 JOB_ID 字段其实是 container hash id',
	`STEP_ID` varchar(64) NULL COMMENT '用户填写的插件id',
    `MODE`        varchar(32)          DEFAULT NULL COMMENT 'LogStorageMode',
    `EXECUTE_COUNT` int(11)     NOT NULL COMMENT '执行次数',
    `FINISHED`      bit(1)      NOT NULL DEFAULT b'0' COMMENT 'build is finished or not',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `BUILD_ID_2` (`BUILD_ID`, `TAG`, `SUB_TAG`, `EXECUTE_COUNT`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='构建日志打印状态表';

-- ----------------------------
-- Table structure for T_LOG_SUBTAGS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_LOG_SUBTAGS`
(
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
  `TAG` varchar(64) NOT NULL DEFAULT '' COMMENT '插件标签',
  `SUB_TAGS` text NOT NULL COMMENT '插件子标签',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `BUILD_ID_2` (`BUILD_ID`,`TAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='构建日志子标签表';

SET FOREIGN_KEY_CHECKS = 1;
