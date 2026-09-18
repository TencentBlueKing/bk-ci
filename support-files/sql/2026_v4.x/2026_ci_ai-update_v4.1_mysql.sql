USE devops_ci_ai;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_ai_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_ai_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AI_SESSION'
                    AND COLUMN_NAME = 'PIPELINE_ID') THEN
        ALTER TABLE `T_AI_SESSION`
            ADD COLUMN `PIPELINE_ID` varchar(64) DEFAULT NULL
                COMMENT '流水线ID，空=项目级或公共会话' AFTER `PROJECT_ID`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_AI_SESSION'
                    AND INDEX_NAME = 'IDX_USER_SCOPE') THEN
        ALTER TABLE `T_AI_SESSION`
            ADD INDEX `IDX_USER_SCOPE` (`USER_ID`, `PROJECT_ID`, `PIPELINE_ID`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_ai_schema_update();
