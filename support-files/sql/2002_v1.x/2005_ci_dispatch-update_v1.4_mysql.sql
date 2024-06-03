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
            AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_IP_INFO'
            AND COLUMN_NAME = 'CLUSTER_NAME') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_IP_INFO'
                        AND COLUMN_NAME = 'CLUSTER_NAME'
                        AND COLUMN_TYPE = 'varchar(64)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO MODIFY COLUMN CLUSTER_NAME varchar(64) DEFAULT 'COMMON' COMMENT '集群名称';
        END IF;
    ELSE
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO ADD COLUMN CLUSTER_NAME varchar(64) DEFAULT 'COMMON' COMMENT '集群名称';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
