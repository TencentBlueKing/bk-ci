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
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();