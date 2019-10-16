USE devops_ci_artifactory;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE IF NOT EXISTS `T_TIPELINE_ARTIFACETORY_INFO`
(
    `ID`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `PIPELINE_ID`      varchar(34)  NOT NULL DEFAULT '',
    `BUILD_ID`         varchar(34)  NOT NULL DEFAULT '',
    `PROJECT_ID`       varchar(64)  NOT NULL DEFAULT '',
    `BUNDLE_ID`        varchar(200) NULL     DEFAULT NULL,
    `BUILD_NUM`        int(11)      NOT NULL DEFAULT 0,
    `NAME`             varchar(100) NOT NULL DEFAULT '',
    `FULL_NAME`        varchar(200) NOT NULL DEFAULT '',
    `PATH`             varchar(200) NOT NULL DEFAULT '',
    `FULL_PATH`        varchar(200) NOT NULL DEFAULT '',
    `SIZE`             int(10)      NOT NULL DEFAULT 0,
    `MODIFIED_TIME`    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ARTIFACTORY_TYPE` varchar(20)  NOT NULL DEFAULT '',
    `PROPERTIES`       text         NOT NULL,
    `APP_VERSION`      varchar(50)  NOT NULL DEFAULT '',
    `DATA_FROM`        tinyint(2)   NULL     DEFAULT 0,
    PRIMARY KEY (`ID`) USING BTREE,
    INDEX `PIPELINE_ID` (`PIPELINE_ID`) USING BTREE,
    INDEX `MODIFIED_TIME` (`MODIFIED_TIME`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4;
-- ----------------------------
-- Table structure for T_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_TOKEN`
(
    `ID`               bigint(20)  NOT NULL AUTO_INCREMENT,
    `USER_ID`          varchar(64) NOT NULL,
    `PROJECT_ID`       varchar(32) NOT NULL,
    `ARTIFACTORY_TYPE` varchar(32) NOT NULL,
    `PATH`             text        NOT NULL,
    `TOKEN`            varchar(64) NOT NULL,
    `EXPIRE_TIME`      datetime    NOT NULL,
    `CREATE_TIME`      datetime    NOT NULL,
    `UPDATE_TIME`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `TOKEN` (`TOKEN`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
