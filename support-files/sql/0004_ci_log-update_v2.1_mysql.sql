USE devops_ci_log;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_log_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_log_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_INDICES_V2'
                    AND COLUMN_NAME = 'LOG_CLUSTER_NAME') THEN
        ALTER TABLE T_LOG_INDICES_V2
            ADD COLUMN `LOG_CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' AFTER ENABLE;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_INDICES_V2'
                    AND COLUMN_NAME = 'USE_CLUSTER') THEN
        ALTER TABLE `T_LOG_INDICES_V2`
        	ADD COLUMN `USE_CLUSTER` bit(1) NOT NULL DEFAULT b'0';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_log_schema_update();

CREATE TABLE IF NOT EXISTS `T_LOG_INDICES_V2`
(
    `ID`            bigint(20)  NOT NULL AUTO_INCREMENT,
    `BUILD_ID`      varchar(64) NOT NULL,
    `INDEX_NAME`    varchar(20) NOT NULL,
    `LAST_LINE_NUM` bigint(20)  NOT NULL DEFAULT '1' COMMENT '最后行号',
    `CREATED_TIME`  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATED_TIME`  timestamp   NOT NULL DEFAULT '2019-11-11 00:00:00' COMMENT '修改时间',
    `ENABLE`        bit(1)      NOT NULL DEFAULT b'0' COMMENT 'build is enable v2 or not',
    `LOG_CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT 'multi es log cluster name',
    `USE_CLUSTER` bit(1) NOT NULL DEFAULT b'0' COMMENT 'use multi es log cluster or not',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `BUILD_ID` (`BUILD_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;