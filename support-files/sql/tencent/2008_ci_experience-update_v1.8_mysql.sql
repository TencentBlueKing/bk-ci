USE devops_ci_experience;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_experience_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_experience_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_EXPERIENCE'
                        AND COLUMN_NAME = 'PIPELINE_ID') THEN
        ALTER TABLE T_EXPERIENCE ADD COLUMN PIPELINE_ID varchar(34) NOT NULL DEFAULT '' COMMENT '流水线ID';
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_EXPERIENCE'
                  AND INDEX_NAME = 'IDX_PIPELINE_ID') THEN
      ALTER TABLE T_EXPERIENCE ADD INDEX IDX_PIPELINE_ID (`PIPELINE_ID`);
  END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_EXPERIENCE'
                        AND COLUMN_NAME = 'BUILD_ID') THEN
        ALTER TABLE T_EXPERIENCE ADD COLUMN BUILD_ID varchar(34) NOT NULL DEFAULT '' COMMENT '构建ID';
  END IF;

  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'T_EXPERIENCE'
                  AND INDEX_NAME = 'IDX_BUILD_ID') THEN
      ALTER TABLE T_EXPERIENCE ADD INDEX IDX_BUILD_ID (`BUILD_ID`);
  END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_EXPERIENCE_PUBLIC'
                        AND COLUMN_NAME = 'VERSION') THEN
      ALTER TABLE `T_EXPERIENCE_PUBLIC` ADD COLUMN `VERSION` VARCHAR(20) DEFAULT '' COMMENT '体验版本号';
  END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_experience_schema_update();
