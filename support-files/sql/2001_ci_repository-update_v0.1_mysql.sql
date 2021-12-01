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
            ADD COLUMN `CREDENTIAL_ID` varchar(128) NULL COMMENT '凭据ID' AFTER REPOSITORY_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_GITHUB'
                        AND COLUMN_NAME = 'CREDENTIAL_ID'
                        AND COLUMN_TYPE = 'varchar(128)') THEN
        ALTER TABLE T_REPOSITORY_GITHUB
            CHANGE `CREDENTIAL_ID` `CREDENTIAL_ID` varchar(128) NULL AFTER REPOSITORY_ID;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_REPOSITORY_GTI_TOKEN') THEN
        IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN') THEN
            RENAME TABLE T_REPOSITORY_GTI_TOKEN TO T_REPOSITORY_GIT_TOKEN;
        END IF;
    END IF;


    IF EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPOSITORY_GIT_TOKEN'
                        AND COLUMN_NAME = 'CREATE_TIME') THEN
            ALTER TABLE `T_REPOSITORY_GIT_TOKEN`
        	    ADD COLUMN `CREATE_TIME` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'token的创建时间';
        END IF;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_repository_schema_update();

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GIT_TOKEN`
(
    `ID`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `USER_ID`       varchar(64) DEFAULT NULL COMMENT '用户ID',
    `ACCESS_TOKEN`  varchar(96) DEFAULT NULL COMMENT '权限Token',
    `REFRESH_TOKEN` varchar(96) DEFAULT NULL COMMENT '刷新token',
    `TOKEN_TYPE`    varchar(64) DEFAULT NULL COMMENT 'token类型',
    `EXPIRES_IN`    bigint(20)  DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `USER_ID` (`USER_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='';