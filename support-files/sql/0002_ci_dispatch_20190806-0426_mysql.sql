USE devops_ci_dispatch;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_DISPATCH_MACHINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_MACHINE` (
  `MACHINE_ID` int(11) NOT NULL AUTO_INCREMENT,
  `MACHINE_IP` varchar(128) NOT NULL,
  `MACHINE_NAME` varchar(128) NOT NULL,
  `MACHINE_USERNAME` varchar(128) NOT NULL,
  `MACHINE_PASSWORD` varchar(128) NOT NULL,
  `MACHINE_CREATED_TIME` datetime NOT NULL,
  `MACHINE_UPDATED_TIME` datetime NOT NULL,
  `CURRENT_VM_RUN` int(11) NOT NULL DEFAULT '0',
  `MAX_VM_RUN` int(11) NOT NULL DEFAULT '1',
  PRIMARY KEY (`MACHINE_ID`),
  UNIQUE KEY `MACHINE_IP` (`MACHINE_IP`),
  UNIQUE KEY `MACHINE_NAME` (`MACHINE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_PIPELINE_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_BUILD` (
  `ID` int(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `BUILD_ID` varchar(34) NOT NULL,
  `VM_SEQ_ID` varchar(34) NOT NULL DEFAULT '',
  `VM_ID` int(20) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `STATUS` int(11) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_PIPELINE_DOCKER_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_BUILD` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BUILD_ID` varchar(64) NOT NULL,
  `VM_SEQ_ID` int(20) NOT NULL,
  `SECRET_KEY` varchar(64) NOT NULL DEFAULT '',
  `STATUS` int(11) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `ZONE` varchar(128) DEFAULT NULL,
  `PROJECT_ID` varchar(34) DEFAULT '',
  `PIPELINE_ID` varchar(34) DEFAULT '',
  `DISPATCH_MESSAGE` varchar(4096) DEFAULT '',
  `STARTUP_MESSAGE` text,
  `ROUTE_KEY` varchar(64) DEFAULT '',
  `DOCKER_INST_ID` bigint(20) DEFAULT NULL,
  `VERSION_ID` int(20) DEFAULT NULL,
  `TEMPLATE_ID` int(20) DEFAULT NULL,
  `NAMESPACE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `BUILD_ID` (`BUILD_ID`,`VM_SEQ_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE` (
  `HOST_IP` varchar(128) NOT NULL,
  `ZONE` varchar(128) NOT NULL,
  `ENABLE` tinyint(1) DEFAULT '1',
  `REMARK` varchar(1024) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `TYPE` int(11) NOT NULL DEFAULT '0',
  `ROUTE_KEY` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`HOST_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_PIPELINE_DOCKER_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_TASK` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) NOT NULL,
  `AGENT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL DEFAULT '',
  `BUILD_ID` varchar(34) NOT NULL,
  `VM_SEQ_ID` int(20) NOT NULL,
  `STATUS` int(11) NOT NULL,
  `SECRET_KEY` varchar(128) NOT NULL,
  `IMAGE_NAME` varchar(1024) NOT NULL,
  `CHANNEL_CODE` varchar(128) DEFAULT NULL,
  `HOST_TAG` varchar(128) DEFAULT NULL,
  `CONTAINER_ID` varchar(128) DEFAULT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `ZONE`          varchar(128) DEFAULT NULL,
  `REGISTRY_USER` varchar(128) DEFAULT NULL,
  `REGISTRY_PWD`  varchar(128) DEFAULT NULL,
  `IMAGE_TYPE`    varchar(128) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `BUILD_ID` (`BUILD_ID`,`VM_SEQ_ID`),
  KEY `UPDATED_TIME` (`UPDATED_TIME`),
  KEY `STATUS` (`STATUS`),
  KEY `HOST_TAG` (`HOST_TAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_THIRDPARTY_AGENT_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_THIRDPARTY_AGENT_BUILD` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) NOT NULL,
  `AGENT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL DEFAULT '',
  `BUILD_ID` varchar(34) NOT NULL,
  `VM_SEQ_ID` varchar(34) NOT NULL,
  `STATUS` int(11) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `WORKSPACE` varchar(4096) DEFAULT NULL,
  `BUILD_NUM` int(20) DEFAULT '0',
  `PIPELINE_NAME` varchar(255) DEFAULT '',
  `TASK_NAME` varchar(255) DEFAULT '',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `BUILD_ID` (`BUILD_ID`,`VM_SEQ_ID`),
  KEY `idx_agent_id` (`AGENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_TEMPLATE`
(
    `ID`                int(20)     NOT NULL AUTO_INCREMENT,
    `VERSION_ID`        int(20)     NOT NULL,
    `SHOW_VERSION_ID`   int(20)     NOT NULL,
    `SHOW_VERSION_NAME` varchar(64) NOT NULL,
    `DEPLOYMENT_ID`     int(20)     NOT NULL,
    `DEPLOYMENT_NAME`   varchar(64) NOT NULL,
    `CC_APP_ID`         bigint(20)  NOT NULL,
    `BCS_PROJECT_ID`    varchar(64) NOT NULL,
    `CLUSTER_ID`        varchar(64) NOT NULL,
    `CREATED_TIME`      datetime    NOT NULL,
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_VM`
(
    `VM_ID`               int(11)      NOT NULL AUTO_INCREMENT,
    `VM_MACHINE_ID`       int(11)      NOT NULL,
    `VM_IP`               varchar(128) NOT NULL,
    `VM_NAME`             varchar(128) NOT NULL,
    `VM_OS`               varchar(64)  NOT NULL,
    `VM_OS_VERSION`       varchar(64)  NOT NULL DEFAULT '',
    `VM_CPU`              varchar(64)  NOT NULL,
    `VM_MEMORY`           varchar(64)  NOT NULL,
    `VM_TYPE_ID`          int(11)      NOT NULL,
    `VM_MAINTAIN`         tinyint(1)   NOT NULL DEFAULT '0',
    `VM_MANAGER_USERNAME` varchar(128) NOT NULL DEFAULT '',
    `VM_MANAGER_PASSWD`   varchar(128) NOT NULL DEFAULT '' COMMENT 'SUDO Password',
    `VM_USERNAME`         varchar(128) NOT NULL DEFAULT '',
    `VM_PASSWD`           varchar(128) NOT NULL DEFAULT '',
    `VM_CREATED_TIME`     datetime     NOT NULL,
    `VM_UPDATED_TIME`     datetime     NOT NULL,
    PRIMARY KEY (`VM_ID`),
    UNIQUE KEY `VM_IP` (`VM_IP`),
    UNIQUE KEY `VM_NAME` (`VM_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_VM`
(
    `PIPELINE_ID` varchar(64) NOT NULL,
    `VM_NAMES`    text        NOT NULL,
    `VM_SEQ_ID`   int(20)     NOT NULL DEFAULT '-1',
    PRIMARY KEY (`PIPELINE_ID`, `VM_SEQ_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PRIVATE_VM`
(
    `VM_ID`      int(11)     NOT NULL,
    `PROJECT_ID` varchar(64) NOT NULL,
    PRIMARY KEY (`VM_ID`),
    UNIQUE KEY `VM_PROJECT_ID` (`VM_ID`, `PROJECT_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PROJECT_SNAPSHOT`
(
    `PROJECT_ID`          varchar(64) NOT NULL,
    `VM_STARTUP_SNAPSHOT` varchar(64) NOT NULL,
    PRIMARY KEY (`PROJECT_ID`),
    UNIQUE KEY `U_PROJECT_SNAPSHOT` (`PROJECT_ID`, `VM_STARTUP_SNAPSHOT`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_VM_TYPE`
(
    `TYPE_ID`           int(11)     NOT NULL AUTO_INCREMENT,
    `TYPE_NAME`         varchar(64) NOT NULL,
    `TYPE_CREATED_TIME` datetime    NOT NULL,
    `TYPE_UPDATED_TIME` datetime    NOT NULL,
    PRIMARY KEY (`TYPE_ID`),
    UNIQUE KEY `TYPE_NAME` (`TYPE_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_DEBUG`
(
    `ID`            int(11)       NOT NULL AUTO_INCREMENT,
    `PROJECT_ID`    varchar(64)   NOT NULL,
    `PIPELINE_ID`   varchar(34)   NOT NULL DEFAULT '',
    `VM_SEQ_ID`     varchar(34)   NOT NULL,
    `STATUS`        int(11)       NOT NULL,
    `TOKEN`         varchar(128)           DEFAULT NULL,
    `IMAGE_NAME`    varchar(1024) NOT NULL,
    `HOST_TAG`      varchar(128)           DEFAULT NULL,
    `CONTAINER_ID`  varchar(128)           DEFAULT NULL,
    `CREATED_TIME`  datetime      NOT NULL,
    `UPDATED_TIME`  datetime      NOT NULL,
    `ZONE`          varchar(128)           DEFAULT NULL,
    `BUILD_ENV`     varchar(4096)          DEFAULT NULL,
    `REGISTRY_USER` varchar(128)           DEFAULT NULL,
    `REGISTRY_PWD`  varchar(128)           DEFAULT NULL,
    `IMAGE_TYPE`    varchar(128)           DEFAULT NULL,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `PIPELINE_ID` (`PIPELINE_ID`, `VM_SEQ_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_ENABLE`
(
    `PIPELINE_ID` varchar(64) NOT NULL,
    `ENABLE`      tinyint(1)  NOT NULL DEFAULT '0',
    `VM_SEQ_ID`   int(20)     NOT NULL DEFAULT '-1',
    PRIMARY KEY (`PIPELINE_ID`, `VM_SEQ_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_HOST`
(
    `PROJECT_CODE` varchar(128) NOT NULL,
    `HOST_IP`      varchar(128) NOT NULL,
    `REMARK`       varchar(1024)         DEFAULT NULL,
    `CREATED_TIME` datetime     NOT NULL,
    `UPDATED_TIME` datetime     NOT NULL,
    `TYPE`         int(11)      NOT NULL DEFAULT '0',
    `ROUTE_KEY`    varchar(45)           DEFAULT NULL,
    PRIMARY KEY (`PROJECT_CODE`, `HOST_IP`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;
