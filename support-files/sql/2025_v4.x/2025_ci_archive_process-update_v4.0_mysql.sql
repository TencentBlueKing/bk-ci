USE devops_ci_archive_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_archive_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_archive_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- 1. 为 T_TEMPLATE_PIPELINE 表添加多个字段
    --    通过检查第一个字段`STATUS`是否存在来判断是否需要执行整个ALTER语句
    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                        AND COLUMN_NAME = 'STATUS') THEN
        ALTER TABLE `T_TEMPLATE_PIPELINE`
        ADD `STATUS` varchar(32) default 'UPDATED' not null comment '状态',
        ADD `PULL_REQUEST_URL` varchar(512) null comment '合并请求链接';
        ADD `PULL_REQUEST_ID` bigint null comment '合并请求ID'
    END IF;

    IF NOT EXISTS(SELECT 1
          FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = db
            AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
            AND COLUMN_NAME = 'VERSION_CHANGE') THEN
    ALTER TABLE T_PIPELINE_BUILD_HISTORY
        ADD`VERSION_CHANGE` BIT DEFAULT NULL comment '是否发生版本变更';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                    AND COLUMN_NAME = 'VERSION_CHANGE') THEN
    ALTER TABLE T_PIPELINE_BUILD_HISTORY_DEBUG
        ADD`VERSION_CHANGE` BIT DEFAULT NULL comment '是否发生版本变更';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_archive_process_schema_update();
