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
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'REGISTRY_USER') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `REGISTRY_USER` varchar(128) DEFAULT NULL COMMENT '注册用户名' AFTER `ZONE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'REGISTRY_PWD') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `REGISTRY_PWD` varchar(128) DEFAULT NULL COMMENT '注册用户密码' AFTER `REGISTRY_USER`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'IMAGE_TYPE') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `IMAGE_TYPE` varchar(128) DEFAULT NULL COMMENT '注册用户密码' AFTER `REGISTRY_PWD`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'UPDATED_TIME') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `UPDATED_TIME` (`UPDATED_TIME`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'STATUS') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `STATUS` (`STATUS`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND INDEX_NAME = 'HOST_TAG') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD INDEX `HOST_TAG` (`HOST_TAG`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_DEBUG'
                    AND COLUMN_NAME = 'IMAGE_PUBLIC_FLAG') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG
            ADD COLUMN `IMAGE_PUBLIC_FLAG` bit(1) DEFAULT NULL COMMENT '镜像是否为公共镜像：0否1是';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_DEBUG'
                    AND COLUMN_NAME = 'IMAGE_RD_TYPE') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG
            ADD COLUMN `IMAGE_RD_TYPE` tinyint(1) DEFAULT NULL COMMENT '镜像研发来源：0自研1第三方';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'IMAGE_PUBLIC_FLAG') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `IMAGE_PUBLIC_FLAG` bit(1) DEFAULT NULL COMMENT '镜像是否为公共镜像：0否1是';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                    AND COLUMN_NAME = 'IMAGE_RD_TYPE') THEN
        ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK
            ADD COLUMN `IMAGE_RD_TYPE` tinyint(1) DEFAULT NULL COMMENT '镜像研发来源：0自研1第三方';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_BUILD'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_BUILD'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_BUILD MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_BUILD'
                AND COLUMN_NAME = 'VM_ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_BUILD'
                        AND COLUMN_NAME = 'VM_ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_BUILD MODIFY COLUMN VM_ID BIGINT(20) NOT NULL COMMENT '虚拟机ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_TASK'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_TASK MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_DEBUG'
                AND COLUMN_NAME = 'ID') THEN
        IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DISPATCH_PIPELINE_DOCKER_DEBUG'
                        AND COLUMN_NAME = 'ID'
                        AND COLUMN_TYPE = 'bigint(20)') THEN
            ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG MODIFY COLUMN ID BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID';
        END IF;
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
