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
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'OAUTH_OPERATOR') THEN
        ALTER TABLE T_GIT_BASIC_SETTING ADD COLUMN `OAUTH_OPERATOR` varchar(32) NOT NULL DEFAULT '' COMMENT 'OAUTH身份的修改者';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'ENABLE_COMMIT_CHECK') THEN
        ALTER TABLE T_GIT_BASIC_SETTING ADD COLUMN `ENABLE_COMMIT_CHECK` bit(1) NOT NULL DEFAULT 1 COMMENT '项目中的构建是否发送commitcheck';
    END IF;


    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                        AND COLUMN_NAME = 'PATH_WITH_NAME_SPACE') THEN
    ALTER TABLE T_GIT_BASIC_SETTING
            ADD COLUMN `PATH_WITH_NAME_SPACE` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '带有名空间的项目路径';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                        AND COLUMN_NAME = 'NAME_WITH_NAME_SPACE') THEN
    ALTER TABLE T_GIT_BASIC_SETTING
            ADD COLUMN `NAME_WITH_NAME_SPACE` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '带有名空间的项目名称';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'ENABLE_MR_COMMENT') THEN
        ALTER TABLE T_GIT_BASIC_SETTING ADD COLUMN `ENABLE_MR_COMMENT` bit(1) NOT NULL DEFAULT 1 COMMENT '项目中的MR是否发送评论';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_stream_schema_update();
