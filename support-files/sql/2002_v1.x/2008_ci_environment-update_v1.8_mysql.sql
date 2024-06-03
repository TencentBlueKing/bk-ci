USE devops_ci_environment;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF  EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AGENT_PIPELINE_REF'
                    AND COLUMN_NAME = 'LAST_BUILD_TIME') THEN

        ALTER TABLE `T_AGENT_PIPELINE_REF`
            MODIFY COLUMN `LAST_BUILD_TIME` datetime DEFAULT NULL COMMENT '最近构建时间';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
