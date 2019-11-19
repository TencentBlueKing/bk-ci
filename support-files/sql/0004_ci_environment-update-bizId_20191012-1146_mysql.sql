USE devops_ci_environment;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `T_AGENT_FAILURE_NOTIFY_USER`
(
    `ID`           bigint(20) NOT NULL AUTO_INCREMENT,
    `USER_ID`      varchar(32) DEFAULT '',
    `NOTIFY_TYPES` varchar(32) DEFAULT '',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `USER_ID` (`USER_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS`
(
    `PROJECT_ID`   varchar(64) NOT NULL,
    `ENALBE`       tinyint(1) DEFAULT NULL,
    `CREATED_TIME` datetime    NOT NULL,
    `UPDATED_TIME` datetime    NOT NULL,
    PRIMARY KEY (`PROJECT_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'BIZ_ID') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `BIZ_ID` bigint(20) DEFAULT NULL;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'BIZ_ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
        ALTER TABLE `T_NODE`
            CHANGE `BIZ_ID` `BIZ_ID` bigint(20) DEFAULT NULL;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();