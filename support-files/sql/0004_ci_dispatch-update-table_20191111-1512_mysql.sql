USE devops_ci_dispatch;
SET NAMES utf8mb4;

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


DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'REGISTRY_USER') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `REGISTRY_USER` varchar(128) DEFAULT NULL AFTER `ZONE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'REGISTRY_PWD') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `REGISTRY_PWD` varchar(128) DEFAULT NULL AFTER `REGISTRY_USER`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'IMAGE_TYPE') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `IMAGE_TYPE` varchar(128) DEFAULT NULL AFTER `REGISTRY_PWD`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'UPDATED_TIME') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `UPDATED_TIME` (`UPDATED_TIME`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'STATUS') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `STATUS` (`STATUS`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'HOST_TAG') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `HOST_TAG` (`HOST_TAG`);
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();