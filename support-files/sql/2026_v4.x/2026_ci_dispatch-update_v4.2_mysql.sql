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
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'IDX_PROJECT_ENV_JOB') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD` ADD INDEX `IDX_PROJECT_ENV_JOB` (PROJECT_ID, ENV_ID, JOB_ID, PIPELINE_ID);
    END IF;

    IF EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                     AND INDEX_NAME = 'BUILD_ID') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD`
            DROP INDEX BUILD_ID;
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                     AND INDEX_NAME = 'IDX_BVE') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD`
            ADD INDEX `IDX_BVE` (`BUILD_ID`, `VM_SEQ_ID`, `EXECUTE_COUNT`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                     AND INDEX_NAME = 'IDX_ENV_ID') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD`
            ADD INDEX `IDX_ENV_ID`(`ENV_ID`);
    END IF;

    -- Job 并发查询先按配额维度 + 活跃状态筛选，避免扫描整条流水线的历史记录。
    -- 保留旧索引；用索引元数据判断，重复执行不重复创建。
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DISPATCH_THIRDPARTY_AGENT_BUILD'
                    AND INDEX_NAME = 'IDX_PROJECT_ENV_JOB_STATUS') THEN
    ALTER TABLE `T_DISPATCH_THIRDPARTY_AGENT_BUILD`
            ADD INDEX `IDX_PROJECT_ENV_JOB_STATUS` (`PROJECT_ID`, `ENV_ID`, `JOB_ID`, `PIPELINE_ID`, `STATUS`);
    END IF;

    COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_schema_update();
