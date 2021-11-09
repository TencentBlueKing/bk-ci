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
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'enable_idc') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `enable_idc` bit(1) DEFAULT NULL COMMENT '是否支持IDC构建机' AFTER `enable_external`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'CHANNEL') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `CHANNEL` varchar(32) NOT NULL DEFAULT 'BS' COMMENT '项目渠道' AFTER `enabled`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'pipeline_limit') THEN
        ALTER TABLE T_PROJECT
            ADD COLUMN `pipeline_limit` int(10) DEFAULT 500 COMMENT '流水线数量上限';
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE'
                    AND COLUMN_NAME = 'logo_url') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `logo_url` varchar(256) DEFAULT NULL COMMENT 'LOGO URL地址' AFTER `gray_js_url`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE'
                    AND COLUMN_NAME = 'web_socket') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `web_socket` text COMMENT '支持webSocket的页面' AFTER `logo_url`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE'
                    AND COLUMN_NAME = 'english_name') THEN
        ALTER TABLE T_SERVICE
            ADD COLUMN `english_name` varchar(64) DEFAULT NULL COMMENT '英文名称' AFTER `name`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE_TYPE'
                    AND COLUMN_NAME = 'english_title') THEN
        ALTER TABLE T_SERVICE_TYPE
            ADD COLUMN `english_title` varchar(64) DEFAULT NULL COMMENT '英文邮件标题' AFTER `title`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ACTIVITY'
                    AND COLUMN_NAME = 'ENGLISH_NAME') THEN
        ALTER TABLE T_ACTIVITY
            ADD COLUMN `ENGLISH_NAME` varchar(128) DEFAULT NULL COMMENT '英文名' AFTER `NAME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SERVICE'
                    AND COLUMN_NAME = 'gray_iframe_url') THEN
        ALTER TABLE T_SERVICE MODIFY COLUMN `iframe_url` varchar(255) DEFAULT NULL COMMENT 'iframe Url地址';
        ALTER TABLE T_SERVICE MODIFY COLUMN `css_url` varchar(255) DEFAULT NULL COMMENT 'css Url地址';
        ALTER TABLE T_SERVICE MODIFY COLUMN `js_url` varchar(255) DEFAULT NULL COMMENT 'js Url地址';
        ALTER TABLE T_SERVICE MODIFY COLUMN `gray_css_url` varchar(255) DEFAULT NULL COMMENT '灰度css Url地址';
        ALTER TABLE T_SERVICE MODIFY COLUMN `gray_js_url` varchar(255) DEFAULT NULL COMMENT '灰度js Url地址';
        ALTER TABLE T_SERVICE ADD COLUMN `gray_iframe_url` varchar(255) DEFAULT NULL COMMENT '灰度iframe Url地址' AFTER `weight`;
    END IF;


    IF EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'project_name'
                    AND COLLATION_NAME= 'utf8mb4_general_ci') THEN
        ALTER TABLE T_PROJECT MODIFY COLUMN `project_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '项目名称';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();