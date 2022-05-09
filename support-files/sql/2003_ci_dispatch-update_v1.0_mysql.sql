USE devops_ci_dispatch;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_DEBUG'
                    AND COLUMN_NAME = 'POOL_NO') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG
            ADD COLUMN `POOL_NO` int(11) DEFAULT 0 COMMENT '构建池序号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_BUILD'
                    AND COLUMN_NAME = 'DOCKER_IP') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_BUILD
            ADD COLUMN `DOCKER_IP` VARCHAR(64) DEFAULT '' COMMENT '构建机IP';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_BUILD'
                    AND COLUMN_NAME = 'CONTAINER_ID') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_BUILD
            ADD COLUMN `CONTAINER_ID` VARCHAR(128) DEFAULT '' COMMENT '构建容器ID';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_BUILD'
                    AND COLUMN_NAME = 'POOL_NO') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_BUILD
            ADD COLUMN `POOL_NO` INT(11) DEFAULT 0 COMMENT '构建容器池序号';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'idx_pipeline_id') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD INDEX idx_pipeline_id (PIPELINE_ID);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'idx_status') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD INDEX idx_status (STATUS);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'IDX_PROJECT_PIPELINE_SEQ_STATUS_TIME') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD INDEX IDX_PROJECT_PIPELINE_SEQ_STATUS_TIME (`PROJECT_ID`, `PIPELINE_ID`, `VM_SEQ_ID`, `STATUS`, `CREATED_TIME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_HOST'
                    AND INDEX_NAME = 'uni_project_code') THEN
        ALTER TABLE `T_DISPATCH_PIPELINE_DOCKER_HOST` ADD UNIQUE INDEX uni_project_code (PROJECT_CODE);
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();