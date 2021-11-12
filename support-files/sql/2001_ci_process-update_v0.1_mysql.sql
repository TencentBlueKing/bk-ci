USE devops_ci_process;
SET NAMES utf8mb4;


DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                    AND COLUMN_NAME = 'PARAM') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            ADD COLUMN `PARAM` mediumtext NOT NULL COMMENT '参数' AFTER BUILD_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                        AND COLUMN_NAME = 'PARAM'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            CHANGE `PARAM` `PARAM` mediumtext NOT NULL AFTER BUILD_ID;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'BUILD_PARAMETERS') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `BUILD_PARAMETERS` mediumtext NULL COMMENT '构建环境参数' AFTER EXECUTE_TIME;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                        AND COLUMN_NAME = 'BUILD_PARAMETERS'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            CHANGE `BUILD_PARAMETERS` `BUILD_PARAMETERS` mediumtext NULL AFTER EXECUTE_TIME;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'TASK_PARAMS') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `TASK_PARAMS` mediumtext NULL COMMENT '任务参数集合' AFTER TASK_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'TASK_PARAMS'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            CHANGE `TASK_PARAMS` `TASK_PARAMS` mediumtext NULL AFTER TASK_ID;
    END IF;
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ADDITIONAL_OPTIONS') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ADDITIONAL_OPTIONS` mediumtext NULL COMMENT '其他选项' AFTER CONTAINER_TYPE;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'ADDITIONAL_OPTIONS'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            CHANGE `ADDITIONAL_OPTIONS` `ADDITIONAL_OPTIONS` mediumtext NULL AFTER CONTAINER_TYPE;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'NAME') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `NAME` mediumtext NULL COMMENT '名称' AFTER RUN_TYPE;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'NAME'
                        AND COLUMN_TYPE = 'varchar(255)') THEN
        ALTER TABLE T_PIPELINE_SETTING
            CHANGE `NAME` `NAME` varchar(255) NULL AFTER RUN_TYPE;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'SUCCESS_RECEIVER') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `SUCCESS_RECEIVER` mediumtext NULL COMMENT '成功接受者' AFTER NAME;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'SUCCESS_RECEIVER'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_SETTING
            CHANGE `SUCCESS_RECEIVER` `SUCCESS_RECEIVER` mediumtext NULL AFTER NAME;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'FAIL_RECEIVER') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `FAIL_RECEIVER` mediumtext NULL COMMENT '失败接受者' AFTER SUCCESS_RECEIVER;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'FAIL_RECEIVER'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_SETTING
            CHANGE `FAIL_RECEIVER` `FAIL_RECEIVER` mediumtext NULL AFTER SUCCESS_RECEIVER;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'SUCCESS_GROUP') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `SUCCESS_GROUP` mediumtext NULL COMMENT '成功接收者' AFTER FAIL_RECEIVER;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'SUCCESS_GROUP'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_SETTING
            CHANGE `SUCCESS_GROUP` `SUCCESS_GROUP` mediumtext NULL AFTER FAIL_RECEIVER;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'FAIL_GROUP') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `FAIL_GROUP` mediumtext NULL COMMENT '失败组' AFTER SUCCESS_GROUP;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'FAIL_GROUP'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_SETTING
            CHANGE `FAIL_GROUP` `FAIL_GROUP` mediumtext NULL AFTER SUCCESS_GROUP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND COLUMN_NAME = 'FILTERS') THEN
        ALTER TABLE T_PIPELINE_VIEW
            ADD COLUMN `FILTERS` mediumtext NULL COMMENT '过滤器' AFTER LOGIC;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_VIEW'
                        AND COLUMN_NAME = 'FILTERS'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_VIEW
            CHANGE `FILTERS` `FILTERS` mediumtext NULL AFTER LOGIC;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW_USER_SETTINGS'
                    AND COLUMN_NAME = 'SETTINGS') THEN
        ALTER TABLE T_PIPELINE_VIEW_USER_SETTINGS
            ADD COLUMN `SETTINGS` mediumtext NULL COMMENT '属性配置表' AFTER PROJECT_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_VIEW_USER_SETTINGS'
                        AND COLUMN_NAME = 'SETTINGS'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_PIPELINE_VIEW_USER_SETTINGS
            CHANGE `SETTINGS` `SETTINGS` mediumtext NULL AFTER PROJECT_ID;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND COLUMN_NAME = 'TYPE') THEN
        ALTER TABLE T_REPORT
            ADD COLUMN `TYPE` varchar(32) NOT NULL DEFAULT 'INTERNAL' AFTER ELEMENT_ID;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPORT'
                        AND COLUMN_NAME = 'TYPE'
                        AND COLUMN_TYPE = 'varchar(32)') THEN
        ALTER TABLE T_REPORT
            CHANGE `TYPE` `TYPE` varchar(32) NOT NULL DEFAULT 'INTERNAL' AFTER ELEMENT_ID;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REPORT'
                    AND COLUMN_NAME = 'INDEX_FILE') THEN
        ALTER TABLE T_REPORT
            ADD COLUMN `INDEX_FILE` mediumtext NOT NULL AFTER TYPE;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REPORT'
                        AND COLUMN_NAME = 'INDEX_FILE'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_REPORT
            CHANGE `INDEX_FILE` `INDEX_FILE` mediumtext NOT NULL AFTER TYPE;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE'
                    AND INDEX_NAME = 'TYPE') THEN
        ALTER TABLE T_TEMPLATE
            ADD INDEX TYPE (`TYPE`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE'
                    AND INDEX_NAME = 'ID') THEN
        ALTER TABLE T_TEMPLATE
            ADD INDEX ID (`ID`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'PARAM') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD COLUMN `PARAM` mediumtext AFTER BUILD_NO;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'PARAM'
                        AND COLUMN_TYPE = 'mediumtext') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            CHANGE `PARAM` `PARAM` mediumtext AFTER BUILD_NO;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR
            ADD COLUMN `PROJECT_ID` varchar(64) DEFAULT NULL AFTER `VALUE`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND COLUMN_NAME = 'PIPELINE_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR
            ADD COLUMN `PIPELINE_ID` varchar(64) DEFAULT NULL COMMENT '流水线ID' AFTER `PROJECT_ID`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND INDEX_NAME = 'IDX_SEARCH_BUILD_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR
            ADD INDEX `IDX_SEARCH_BUILD_ID` (`PROJECT_ID`,`PIPELINE_ID`, `KEY`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ERROR_TYPE') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ERROR_TYPE` int(11) DEFAULT NULL COMMENT '错误类型' AFTER `TOTAL_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ERROR_CODE') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ERROR_CODE` int(11) DEFAULT NULL COMMENT '错误码' AFTER `ERROR_TYPE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ERROR_MSG') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ERROR_MSG` text DEFAULT NULL AFTER `ERROR_CODE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'CONTAINER_HASH_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `CONTAINER_HASH_ID` varchar(64) DEFAULT NULL AFTER `ERROR_MSG`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'ERROR_TYPE') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `ERROR_TYPE` int(11) DEFAULT NULL COMMENT '错误类型' AFTER `RECOMMEND_VERSION`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'ERROR_CODE') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `ERROR_CODE` int(11) DEFAULT NULL COMMENT '错误码' AFTER `ERROR_TYPE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'ERROR_MSG') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `ERROR_MSG` text DEFAULT NULL AFTER `ERROR_CODE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                    AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            ADD COLUMN `PROJECT_ID` varchar(64) DEFAULT NULL;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                    AND COLUMN_NAME = 'PIPELINE_ID') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            ADD COLUMN `PIPELINE_ID` varchar(64) DEFAULT NULL COMMENT '流水线ID';
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                    AND INDEX_NAME = 'IDX_DEL') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            ADD INDEX IDX_DEL (`PROJECT_ID`,`PIPELINE_ID`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND COLUMN_NAME = 'VAR_TYPE') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR ADD COLUMN `VAR_TYPE` VARCHAR(64) COMMENT '变量类型';
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'CREATOR') THEN
        ALTER TABLE T_PIPELINE_RESOURCE ADD COLUMN `CREATOR` varchar(64) DEFAULT NULL;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                    AND COLUMN_NAME = 'CREATE_TIME') THEN
        ALTER TABLE T_PIPELINE_RESOURCE ADD COLUMN `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG') THEN
        ALTER TABLE T_PIPELINE_SETTING ADD COLUMN `SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG` bit(1) NOT NULL DEFAULT b'0';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'FAIL_WECHAT_GROUP_MARKDOWN_FLAG') THEN
        ALTER TABLE T_PIPELINE_SETTING ADD COLUMN `FAIL_WECHAT_GROUP_MARKDOWN_FLAG` bit(1) NOT NULL DEFAULT b'0';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'INSTANCE_TYPE') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD COLUMN `INSTANCE_TYPE` VARCHAR(32) NOT NULL DEFAULT 'CONSTRAINT' COMMENT '实例化类型：FREEDOM 自由模式  CONSTRAINT 约束模式' AFTER `PIPELINE_ID`;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'INSTANCE_TYPE'
                        AND COLUMN_TYPE = 'VARCHAR') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            CHANGE `INSTANCE_TYPE` `INSTANCE_TYPE` VARCHAR(32) NOT NULL DEFAULT 'CONSTRAINT' COMMENT '实例化类型：FREEDOM 自由模式  CONSTRAINT 约束模式';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'ROOT_TEMPLATE_ID') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD COLUMN `ROOT_TEMPLATE_ID` VARCHAR(32) NULL COMMENT '源模板ID' AFTER `INSTANCE_TYPE`;
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'ROOT_TEMPLATE_ID'
                        AND COLUMN_TYPE = 'VARCHAR') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            CHANGE `ROOT_TEMPLATE_ID` `ROOT_TEMPLATE_ID` VARCHAR(32) NULL COMMENT '源模板ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND INDEX_NAME = 'ROOT_TEMPLATE_ID') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE
            ADD INDEX ROOT_TEMPLATE_ID (`ROOT_TEMPLATE_ID`);
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_TEMPLATE'
                AND COLUMN_NAME = 'VERSION') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE'
                        AND COLUMN_NAME = 'VERSION'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_TEMPLATE MODIFY COLUMN VERSION BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_PIPELINE_WEBHOOK MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_TEMPLATE'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_TEMPLATE'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_PIPELINE_TEMPLATE MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_BUILD_STARTUP_PARAM MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
