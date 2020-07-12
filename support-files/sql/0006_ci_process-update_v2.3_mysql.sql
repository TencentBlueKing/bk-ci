USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND INDEX_NAME = 'UNIQ_KEY') THEN

        DELETE
        FROM T_PIPELINE_WEBHOOK
        WHERE ID NOT IN
              (SELECT ID FROM (SELECT ID FROM T_PIPELINE_WEBHOOK GROUP BY PIPELINE_ID, REPO_HASH_ID) tmp);

        ALTER TABLE T_PIPELINE_WEBHOOK ADD UNIQUE INDEX `UNIQ_KEY` (`PIPELINE_ID`, `REPO_HASH_ID`);

        IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_WEBHOOK'
                    AND INDEX_NAME = 'fk_T_PIPELINE_WEBHOOK_T_PIPELINE_INFO1_idx') THEN
            ALTER TABLE T_PIPELINE_WEBHOOK DROP INDEX `fk_T_PIPELINE_WEBHOOK_T_PIPELINE_INFO1_idx`;
        END IF;

    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'MAX_PIPELINE_RES_NUM') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `MAX_PIPELINE_RES_NUM` int(11) DEFAULT '50' COLLATE utf8mb4_bin;
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
