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
            AND COLUMN_NAME = 'STATUS') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_IP_INFO'
                        AND COLUMN_NAME = 'STATUS'
                        AND COLUMN_TYPE = 'int(11)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO MODIFY COLUMN STATUS int(11) DEFAULT 1;
        END IF;
    ELSE
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO ADD COLUMN STATUS int(11) DEFAULT 1;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.statistics
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_IP_INFO'
                AND INDEX_NAME = 'UNI_IP') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO DROP INDEX `UNI_IP`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_IP_INFO'
                    AND INDEX_NAME = 'UNI_IP_PORT') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_IP_INFO ADD INDEX `UNI_IP_PORT` (`DOCKER_IP`, `DOCKER_HOST_PORT`);
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
