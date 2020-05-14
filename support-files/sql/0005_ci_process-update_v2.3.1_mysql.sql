USE devops_ci_process;
SET NAMES utf8mb4;

-- Stage预置标签
REPLACE INTO `T_PIPELINE_STAGE_TAG` (`ID`, `STAGE_TAG_NAME`, `WEIGHT`, `CREATOR`, `MODIFIER`, `CREATE_TIME`, `UPDATE_TIME`) VALUES
	('28ee946a59f64949a74f3dee40a1bda4','Build',99,'system','system','2020-03-03 18:07:12','2020-03-19 16:29:38'),
	('53b4d3f38e3e425cb1aaa97aa1b37857','Deploy',0,'system','system','2020-03-19 18:00:04','2020-03-19 18:00:04'),
	('d0a06f6986fa4670af65ccad7bb49d3a','Test',50,'system','system','2020-03-19 16:29:45','2020-03-19 16:29:45');


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


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
