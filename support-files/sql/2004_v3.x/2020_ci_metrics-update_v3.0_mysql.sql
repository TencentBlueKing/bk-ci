USE devops_ci_metrics;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_metrics_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_metrics_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
SELECT DATABASE() INTO db;

IF EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_EPLUS_PIPELINE_METRICS_DATA_DAILY'
                        AND COLUMN_NAME = 'IS_INVALID_PIPELINE') THEN
ALTER TABLE T_EPLUS_PIPELINE_METRICS_DATA_DAILY
    CHANGE IS_INVALID_PIPELINE INVALID_PIPELINE bit(1) NULL COMMENT '是否是无效流水线（连续90失败并且构建次数大于14）';

END IF;

COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_metrics_schema_update();
