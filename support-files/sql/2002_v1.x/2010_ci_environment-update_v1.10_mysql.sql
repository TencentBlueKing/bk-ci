USE devops_ci_environment;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENVIRONMENT_THIRDPARTY_AGENT'
                    AND COLUMN_NAME = 'DOCKER_PARALLEL_TASK_COUNT') THEN

        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`
            ADD COLUMN `DOCKER_PARALLEL_TASK_COUNT` int(11) DEFAULT NULL COMMENT 'Docker构建机并行任务计数';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_NODE'
                AND COLUMN_NAME = 'NODE_STRING_ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'NODE_STRING_ID'
                        AND COLUMN_TYPE = 'varchar(255)') THEN
            ALTER TABLE T_NODE MODIFY COLUMN NODE_STRING_ID varchar(255) DEFAULT NULL COMMENT '节点ID字符串';
        END IF;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENVIRONMENT_SLAVE_GATEWAY'
                    AND COLUMN_NAME = 'FILE_GATEWAY') THEN

        ALTER TABLE `T_ENVIRONMENT_SLAVE_GATEWAY`
            ADD COLUMN `FILE_GATEWAY` varchar(127) DEFAULT NULL COMMENT '文件网关地址';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENVIRONMENT_SLAVE_GATEWAY'
                    AND COLUMN_NAME = 'VISIBILITY') THEN

        ALTER TABLE `T_ENVIRONMENT_SLAVE_GATEWAY`
            ADD COLUMN `VISIBILITY` bit(1) DEFAULT b'0' COMMENT '是否在界面可见';
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
