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
                    AND COLUMN_NAME = 'FILE_GATEWAY') THEN

        ALTER TABLE `T_ENVIRONMENT_THIRDPARTY_AGENT`
            ADD COLUMN `FILE_GATEWAY` varchar(128) DEFAULT '' COMMENT '文件网关路径';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'PIPELINE_REF_COUNT') THEN

        ALTER TABLE `T_NODE`
            ADD COLUMN `PIPELINE_REF_COUNT` int(11) NOT NULL DEFAULT '0' COMMENT '流水线Job引用数';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'LAST_BUILD_TIME') THEN

        ALTER TABLE `T_NODE`
            ADD COLUMN `LAST_BUILD_TIME` datetime DEFAULT NULL COMMENT '最近构建时间';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();