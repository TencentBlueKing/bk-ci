USE devops_ci_project;
SET NAMES utf8mb4;


CREATE TABLE IF NOT EXISTS `T_GRAY_TEST`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `service_id` bigint(20)  DEFAULT NULL COMMENT '服务id',
    `username`   varchar(64) DEFAULT NULL COMMENT '用户',
    `status`     varchar(64) DEFAULT NULL COMMENT '服务状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `service_name` (`service_id`, `username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '',
    `LABEL_NAME`  varchar(45) NOT NULL,
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_inx_tmpl_name` (`LABEL_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '',
    `LABEL_ID`    varchar(32) NOT NULL,
    `PROJECT_ID`  varchar(32) NOT NULL,
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_tmplr_label_id` (`LABEL_ID`),
    KEY `inx_tmplr_project_id` (`PROJECT_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'enable_idc') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `enable_idc` bit(1) DEFAULT NULL AFTER `enable_external`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'CHANNEL') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `CHANNEL` varchar(32) NOT NULL DEFAULT 'BS' AFTER `enabled`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 't_service'
                    AND COLUMN_NAME = 'logo_url') THEN
        ALTER TABLE t_service
            ADD COLUMN `logo_url` varchar(256) DEFAULT NULL AFTER `gray_js_url`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 't_service'
                    AND COLUMN_NAME = 'web_socket') THEN
        ALTER TABLE t_service
            ADD COLUMN `web_socket` text AFTER `logo_url`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();