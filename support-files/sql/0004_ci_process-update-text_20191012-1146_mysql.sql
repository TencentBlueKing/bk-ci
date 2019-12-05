USE devops_ci_process;
SET NAMES utf8mb4;


CREATE TABLE IF NOT EXISTS `T_PROJECT_PIPELINE_CALLBACK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(64) NOT NULL,
  `EVENTS` varchar(255) DEFAULT NULL,
  `CALLBACK_URL` varchar(255) NOT NULL,
  `CREATOR` varchar(64) NOT NULL,
  `UPDATOR` varchar(64) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  `SECRET_TOKEN` text DEFAULT NULL COMMENT 'Send to your with http header: X-DEVOPS-WEBHOOK-TOKEN',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `IDX_PROJECT_CALLBACK` (`PROJECT_ID`, `CALLBACK_URL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
            ADD COLUMN `PARAM` mediumtext NOT NULL AFTER BUILD_ID;
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
            ADD COLUMN `BUILD_PARAMETERS` mediumtext NULL AFTER EXECUTE_TIME;
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
            ADD COLUMN `TASK_PARAMS` mediumtext NULL AFTER TASK_ID;
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
            ADD COLUMN `ADDITIONAL_OPTIONS` mediumtext NULL AFTER CONTAINER_TYPE;
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
            ADD COLUMN `NAME` mediumtext NULL AFTER RUN_TYPE;
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
            ADD COLUMN `SUCCESS_RECEIVER` mediumtext NULL AFTER NAME;
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
            ADD COLUMN `FAIL_RECEIVER` mediumtext NULL AFTER SUCCESS_RECEIVER;
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
            ADD COLUMN `SUCCESS_GROUP` mediumtext NULL AFTER FAIL_RECEIVER;
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
            ADD COLUMN `FAIL_GROUP` mediumtext NULL AFTER SUCCESS_GROUP;
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
            ADD COLUMN `FILTERS` mediumtext NULL AFTER LOGIC;
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
            ADD COLUMN `SETTINGS` mediumtext NULL AFTER PROJECT_ID;
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
            ADD COLUMN `PIPELINE_ID` varchar(64) DEFAULT NULL AFTER `PROJECT_ID`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND INDEX_NAME = 'IDX_SEARCH_BUILDID') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR
            ADD INDEX `IDX_SEARCH_BUILD_ID` (`PROJECT_ID`,`PIPELINE_ID`, `KEY`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ERROR_TYPE') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ERROR_TYPE` int(11) DEFAULT NULL AFTER `TOTAL_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'ERROR_CODE') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK
            ADD COLUMN `ERROR_CODE` int(11) DEFAULT NULL AFTER `ERROR_TYPE`;
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
            ADD COLUMN `ERROR_TYPE` int(11) DEFAULT NULL AFTER `RECOMMEND_VERSION`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'ERROR_CODE') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `ERROR_CODE` int(11) DEFAULT NULL AFTER `ERROR_TYPE`;
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
            ADD COLUMN `PIPELINE_ID` varchar(64) DEFAULT NULL;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_STARTUP_PARAM'
                    AND INDEX_NAME = 'IDX_DEL') THEN
        ALTER TABLE T_BUILD_STARTUP_PARAM
            ADD INDEX IDX_DEL (`PROJECT_ID`,`PIPELINE_ID`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
