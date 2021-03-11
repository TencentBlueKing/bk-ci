USE devops_ci_sign;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_sign_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_sign_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_SIGN_HISTORY'
                    AND INDEX_NAME = 'INX_BUILD_TASK_COUNT') THEN
        ALTER TABLE T_SIGN_HISTORY
            ADD INDEX `INX_BUILD_TASK_COUNT` (`BUILD_ID`,`TASK_ID`,`TASK_EXECUTE_COUNT`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_sign_schema_update();
