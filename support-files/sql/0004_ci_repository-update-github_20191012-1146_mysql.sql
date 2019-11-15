USE devops_ci_repository;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_repository_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_repository_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GITHUB'
                    AND COLUMN_NAME = 'CREDENTIAL_ID') THEN
        ALTER TABLE T_REPOSITORY_GITHUB
            ADD COLUMN `CREDENTIAL_ID` varchar(128) NULL AFTER REPOSITORY_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_GITHUB'
                        AND COLUMN_NAME = 'CREDENTIAL_ID'
                        AND COLUMN_TYPE = 'varchar(128)') THEN
        ALTER TABLE T_REPOSITORY_GITHUB
            CHANGE `CREDENTIAL_ID` `CREDENTIAL_ID` varchar(128) NULL AFTER REPOSITORY_ID;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GIT_TOKEN`
(
    `ID`            bigint(20) NOT NULL AUTO_INCREMENT,
    `USER_ID`       varchar(64) DEFAULT NULL,
    `ACCESS_TOKEN`  varchar(96) DEFAULT NULL,
    `REFRESH_TOKEN` varchar(96) DEFAULT NULL,
    `TOKEN_TYPE`    varchar(64) DEFAULT NULL,
    `EXPIRES_IN`    bigint(20)  DEFAULT NULL,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `USER_ID` (`USER_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;