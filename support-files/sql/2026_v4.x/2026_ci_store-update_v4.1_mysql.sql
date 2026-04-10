USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_CLASSIFY'
                AND COLUMN_NAME = 'SERVICE_SCOPE') THEN
       ALTER TABLE T_CLASSIFY ADD COLUMN `SERVICE_SCOPE` varchar(64)
           DEFAULT '' COMMENT '服务范围，PIPELINE：流水线 CREATIVE_STREAM：创作流';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_CLASSIFY'
                     AND INDEX_NAME = 'UNI_INX_TC_NAME_TYPE_SCOPE') THEN
       ALTER TABLE `T_CLASSIFY` ADD UNIQUE INDEX
           `UNI_INX_TC_NAME_TYPE_SCOPE` (`TYPE`,`SERVICE_SCOPE`,`CLASSIFY_NAME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_CLASSIFY'
                     AND INDEX_NAME = 'UNI_INX_TC_CODE_TYPE_SCOPE') THEN
       ALTER TABLE `T_CLASSIFY` ADD UNIQUE INDEX
           `UNI_INX_TC_CODE_TYPE_SCOPE` (`TYPE`,`SERVICE_SCOPE`,`CLASSIFY_CODE`);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_CLASSIFY'
                   AND INDEX_NAME = 'uni_inx_name_type') THEN
       ALTER TABLE `T_CLASSIFY` DROP INDEX `uni_inx_name_type`;
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_CLASSIFY'
                   AND INDEX_NAME = 'uni_inx_code_type') THEN
       ALTER TABLE `T_CLASSIFY` DROP INDEX `uni_inx_code_type`;
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_LABEL'
                AND COLUMN_NAME = 'SERVICE_SCOPE') THEN
       ALTER TABLE T_LABEL ADD COLUMN `SERVICE_SCOPE` varchar(64)
          DEFAULT '' COMMENT '服务范围，PIPELINE：流水线 CREATIVE_STREAM：创作流';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_LABEL'
                     AND INDEX_NAME = 'UNI_INX_TL_TYPE_NAME_SCOPE') THEN
       ALTER TABLE `T_LABEL` ADD UNIQUE INDEX
           `UNI_INX_TL_TYPE_NAME_SCOPE` (`TYPE`,`SERVICE_SCOPE`,`LABEL_NAME`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_LABEL'
                     AND INDEX_NAME = 'UNI_INX_TL_TYPE_CODE_SCOPE') THEN
       ALTER TABLE `T_LABEL` ADD UNIQUE INDEX
           `UNI_INX_TL_TYPE_CODE_SCOPE` (`TYPE`,`SERVICE_SCOPE`,`LABEL_CODE`);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_LABEL'
                   AND INDEX_NAME = 'UNI_INX_TL_TYPE_NAME') THEN
       ALTER TABLE `T_LABEL` DROP INDEX `UNI_INX_TL_TYPE_NAME`;
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_LABEL'
                   AND INDEX_NAME = 'UNI_INX_TL_TYPE_CODE') THEN
      ALTER TABLE `T_LABEL` DROP INDEX `UNI_INX_TL_TYPE_CODE`;
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'CLASSIFY_ID_MAP') THEN
      ALTER TABLE T_ATOM ADD COLUMN `CLASSIFY_ID_MAP` text
         COMMENT '多服务范围分类映射，JSON格式：{"PIPELINE":"classifyId1","CREATIVE_STREAM":"classifyId2"}';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_ATOM'
                     AND INDEX_NAME = 'inx_ta_latest_status_default_delete') THEN
      ALTER TABLE `T_ATOM` ADD INDEX
        `inx_ta_latest_status_default_delete` (LATEST_FLAG, ATOM_STATUS, DEFAULT_FLAG, DELETE_FLAG);
    END IF;

    IF EXISTS(SELECT 1
                 FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = db
                   AND TABLE_NAME = 'T_ATOM'
                   AND INDEX_NAME = 'inx_tpca_service_code') THEN
      ALTER TABLE `T_ATOM` DROP INDEX `inx_tpca_service_code`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
