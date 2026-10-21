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
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识表达式，取值相同的构建计为同一批';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识表达式，取值相同的构建计为同一批';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_SETTING_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_VERSION
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识表达式，取值相同的构建计为同一批';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_DRAFT_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_SETTING_DRAFT_VERSION
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识表达式，取值相同的构建计为同一批';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识表达式，取值相同的构建计为同一批';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识，启动时解析后的值';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                    AND COLUMN_NAME = 'CONCURRENCY_SUB_GROUP') THEN
        ALTER TABLE T_PIPELINE_BUILD_HISTORY_DEBUG
            ADD COLUMN `CONCURRENCY_SUB_GROUP` varchar(255) DEFAULT NULL COMMENT '同一批标识，启动时解析后的值';
    END IF;

    ## 以前缺失补刀 feat：草稿并发修改交互优化 #10553 setting增加version字段 --- START
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION
            ADD COLUMN `VERSION` int(11) NOT NULL DEFAULT '1' COMMENT '流水线版本号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_DRAFT_VERSION'
                    AND COLUMN_NAME = 'VERSION') THEN
        ALTER TABLE T_PIPELINE_SETTING_DRAFT_VERSION
            ADD COLUMN `VERSION` int(11) NOT NULL DEFAULT '1' COMMENT '流水线版本号';
    END IF;

    -- 仅当主键仍包含旧列 SETTING_VERSION 时才修改(避免重复执行报错)
    IF EXISTS(SELECT 1
              FROM information_schema.KEY_COLUMN_USAGE
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION'
                AND COLUMN_NAME = 'SETTING_VERSION'
                AND CONSTRAINT_NAME = 'PRIMARY') THEN
        ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_DRAFT_VERSION
            DROP PRIMARY KEY,
            ADD PRIMARY KEY (`PROJECT_ID`, `TEMPLATE_ID`, `VERSION`, `DRAFT_VERSION`);
    END IF;

    -- 仅当主键仍包含旧列 SETTING_VERSION 时才修改(避免重复执行报错)
    IF EXISTS(SELECT 1
              FROM information_schema.KEY_COLUMN_USAGE
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_SETTING_DRAFT_VERSION'
                AND COLUMN_NAME = 'SETTING_VERSION'
                AND CONSTRAINT_NAME = 'PRIMARY') THEN
        ALTER TABLE T_PIPELINE_SETTING_DRAFT_VERSION
            DROP PRIMARY KEY,
            ADD PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `VERSION`, `DRAFT_VERSION`);
    END IF;

    ## 以前缺失补刀 feat：草稿并发修改交互优化 #10553 setting增加version字段 --- END

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
