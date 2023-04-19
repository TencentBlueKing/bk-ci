USE devops_ci_stream;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_stream_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_stream_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_REQUEST_EVENT_NOT_BUILD'
                    AND COLUMN_NAME = 'BRANCH') THEN
        ALTER TABLE T_GIT_REQUEST_EVENT_NOT_BUILD ADD COLUMN `BRANCH` varchar(1024) NULL COMMENT 'git分支' AFTER `VERSION`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'GIT_PROJECT_DESC') THEN
        ALTER TABLE T_GIT_BASIC_SETTING 
        ADD COLUMN `GIT_PROJECT_DESC` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'GIT项目的描述信息';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'GIT_PROJECT_AVATAR') THEN
        ALTER TABLE T_GIT_BASIC_SETTING 
        ADD COLUMN `GIT_PROJECT_AVATAR` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'GIT项目的头像信息';
    END IF;

        IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'LAST_CI_INFO') THEN
        ALTER TABLE T_GIT_BASIC_SETTING 
        ADD COLUMN `LAST_CI_INFO` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '最后一次构建的CI信息' AFTER `GIT_PROJECT_AVATAR`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_stream_schema_update();
