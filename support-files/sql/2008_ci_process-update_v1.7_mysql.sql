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
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_DETAIL'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_DETAIL ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'SUB_PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_TASK ADD COLUMN `SUB_PROJECT_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '子流水线项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_LABEL'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_LABEL ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_PIPELINE_LABEL_PIPELINE'
                  AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_LABEL_PIPELINE ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_VIEW_LABEL'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_VIEW_LABEL ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_PAUSE_VALUE'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_PAUSE_VALUE ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_RESOURCE'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_RESOURCE ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_RESOURCE_VERSION'
                    AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_RESOURCE_VERSION ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;
	
	    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_WEBHOOK_QUEUE'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_WEBHOOK_QUEUE ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_TEMPLATE_INSTANCE_ITEM'
                        AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_TEMPLATE_INSTANCE_ITEM ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'PROJECT_ID') THEN
        ALTER TABLE T_TEMPLATE_PIPELINE ADD COLUMN `PROJECT_ID` varchar(64) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_CONTAINER'
                    AND COLUMN_NAME = 'CONTAINER_HASH_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_CONTAINER ADD COLUMN `CONTAINER_HASH_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '容器全局唯一ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_CONTAINER'
                    AND COLUMN_NAME = 'MATRIX_GROUP_FLAG') THEN
        ALTER TABLE T_PIPELINE_BUILD_CONTAINER ADD COLUMN `MATRIX_GROUP_FLAG` BIT(1) DEFAULT NULL COMMENT '是否为构建矩阵';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_CONTAINER'
                    AND COLUMN_NAME = 'MATRIX_GROUP_ID') THEN
        ALTER TABLE T_PIPELINE_BUILD_CONTAINER ADD COLUMN `MATRIX_GROUP_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '所属的矩阵组ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                    AND COLUMN_NAME = 'STEP_ID') THEN
    ALTER TABLE T_PIPELINE_BUILD_TASK ADD COLUMN `STEP_ID` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '标识上下文的自定义ID';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
