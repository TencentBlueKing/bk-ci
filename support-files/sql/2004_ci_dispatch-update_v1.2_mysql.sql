USE devops_ci_dispatch;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                AND COLUMN_NAME = 'CONTAINER_HASH_ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                        AND COLUMN_NAME = 'CONTAINER_HASH_ID'
                        AND COLUMN_TYPE = 'varchar(128)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK MODIFY COLUMN CONTAINER_HASH_ID varchar(128) DEFAULT NULL COMMENT '构建Job唯一标识';
        END IF;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'IDX_AGENTID_STATUS_UPDATE') THEN
        ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD ADD INDEX `IDX_AGENTID_STATUS_UPDATE` (`AGENT_ID`, `STATUS`, `UPDATED_TIME`);
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
