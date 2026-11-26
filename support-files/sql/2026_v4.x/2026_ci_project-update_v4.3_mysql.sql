USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_CONVENTION_BODY') THEN
        CREATE TABLE `T_CONVENTION_BODY` (
          `LANG` varchar(8) NOT NULL COMMENT 'zh 或 en',
          `CONTENT` mediumtext NOT NULL COMMENT '该语言的公约正文',
          `UPDATER` varchar(64) NOT NULL COMMENT '最后保存人',
          `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后保存时间',
          PRIMARY KEY (`LANG`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用公约正文';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_CONVENTION_AGREEMENT') THEN
        CREATE TABLE `T_CONVENTION_AGREEMENT` (
          `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
          `USER_ID` varchar(64) NOT NULL COMMENT '确认人',
          `AGREE_TIME` datetime NOT NULL COMMENT '确认时间',
          PRIMARY KEY (`ID`),
          UNIQUE KEY `UNI_USER_ID` (`USER_ID`),
          KEY `IDX_AGREE_TIME` (`AGREE_TIME`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用公约确认记录';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;
