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
                AND COLUMN_NAME = 'JOB_TYPE_MAP') THEN
      ALTER TABLE T_ATOM ADD COLUMN `JOB_TYPE_MAP` text
        COMMENT '多服务范围Job类型映射，JSON格式：{"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}';
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
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'OS_MAP') THEN
      ALTER TABLE T_ATOM ADD COLUMN `OS_MAP` text
          COMMENT '多JobType操作系统映射，JSON格式：{"AGENT":["WINDOWS","LINUX","MACOS"],"CREATIVE_STREAM":["WINDOWS"]}';
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

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'OWNER_STORE_CODE') THEN
    ALTER TABLE T_ATOM ADD `OWNER_STORE_CODE` varchar(64) DEFAULT NULL COMMENT '归属应用标识';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_ATOM'
                AND COLUMN_NAME = 'VERSION'
                AND CHARACTER_MAXIMUM_LENGTH < 256) THEN
    ALTER TABLE T_ATOM MODIFY COLUMN `VERSION` varchar(256) NOT NULL COMMENT '版本号';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();

-- TAPD trigger: project dropdown + status filter props
-- NOTE: invalidate AtomPropsCacheManager for codeTapdWebHookTrigger after deployment.
UPDATE T_ATOM SET PROPS = '{"input":{"tips":{"label":"","type":"tips-simple","tipStr":"TAPD event triggering requires <a target=\"_blank\", href=\"/ms/repository/api/user/tapd/appInstall\">app installation<a/> in advance"},"workspaceId":{"rule":{},"type":"request-selector","component":"request-selector","required":true,"label":"TAPD Project ID","placeholder":"Please select TAPD Project ID","default":"","url":"/repository/api/user/tapd/workspaces","paramId":"id","paramName":"name","searchable":true,"hasAddItem":false},"eventType":{"rule":{},"required":true,"component":"enum-input","type":"enum-input","label":"Event Type","list":[{"label":"Story","value":"STORY"},{"label":"Bug","value":"BUG"}],"default":"STORY"},"includeEventFrom":{"label":"Event Source to Listen","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"api","name":"API"},{"id":"web","name":"WEB"},{"id":"auto_task","name":"AUTO_TASK"}],"default":["api","web","auto_task"]},"includeStoryAction":{"label":"Actions to Listen","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"create","name":"Create"},{"id":"update","name":"Update"},{"id":"delete","name":"Delete"},{"id":"status_change","name":"Status Change"},{"id":"bug_link","name":"Link Bug"},{"id":"bug_unlink","name":"Unlink Bug"},{"id":"story_link","name":"Link Story"},{"id":"story_unlink","name":"Unlink Story"},{"id":"add_comment","name":"Add Comment"},{"id":"update_comment","name":"Update Comment"},{"id":"delete_comment","name":"Delete Comment"}],"default":["create","update","delete"],"rely":{"operation":"AND","expression":[{"key":"eventType","value":"STORY"}]}},"includeBugAction":{"label":"Actions to Listen","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"create","name":"Create"},{"id":"update","name":"Update"},{"id":"delete","name":"Delete"},{"id":"status_change","name":"Status Change"},{"id":"add_comment","name":"Add Comment"},{"id":"update_comment","name":"Update Comment"},{"id":"delete_comment","name":"Delete Comment"}],"default":["create","update","delete"],"rely":{"operation":"AND","expression":[{"key":"eventType","value":"BUG"}]}},"includePriority":{"rule":{},"type":"vuex-input","required":false,"label":"Listen to the following priorities","desc":"","placeholder":"Separate multiple priorities with commas","default":""},"includeStatuses":{"rule":{},"type":"request-selector","component":"request-selector","required":false,"default":[],"url":"/repository/api/user/tapd/statuses?workspaceId={workspaceId}&eventType={eventType}","paramId":"id","paramName":"name","searchable":true,"multiSelect":true,"hasAddItem":false,"rely":{"operation":"NOT","expression":[{"key":"workspaceId","value":""}]},"label":"Included Statuses","placeholder":"Leave empty to listen to all statuses"},"excludeStatuses":{"rule":{},"type":"request-selector","component":"request-selector","required":false,"default":[],"url":"/repository/api/user/tapd/statuses?workspaceId={workspaceId}&eventType={eventType}","paramId":"id","paramName":"name","searchable":true,"multiSelect":true,"hasAddItem":false,"rely":{"operation":"NOT","expression":[{"key":"workspaceId","value":""}]},"label":"Excluded Statuses","placeholder":"Pipeline will not be triggered when matched"},"userSettings":{"type":"group","label":"User Settings","children":[{"key":"includeUsers","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"Include Users","desc":"If you need to include public accounts, you can define the corresponding users as pipeline variables (separate multiple users with commas), and then reference them via ${{ variable }}","default":[]},{"key":"excludeUsers","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"Exclude Users","desc":"If you need to exclude public accounts, you can define the corresponding users as pipeline variables (separate multiple users with commas), and then reference them via ${{ variable }}","default":[]}]},"labelSettings":{"type":"group","label":"Label Settings","children":[{"key":"includeLabels","type":"groupItem","rule":{},"component":"composite-input","required":false,"multiSelect":true,"prependText":"Include Labels","desc":"Separate multiple labels with commas","default":""},{"key":"excludeLabels","type":"groupItem","rule":{},"component":"composite-input","required":false,"multiSelect":true,"prependText":"Exclude Labels","desc":"Separate multiple labels with commas","default":""}]},"ownerSettings":{"type":"group","label":"Owner Settings","children":[{"key":"includeOwner","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"Include Owners","desc":"If you need to include public accounts, you can define the corresponding users as pipeline variables (separate multiple users with commas), and then reference them via ${{ variable }}","default":[]},{"key":"excludeOwner","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"Exclude Owners","desc":"If you need to exclude public accounts, you can define the corresponding users as pipeline variables (separate multiple users with commas), and then reference them via ${{ variable }}","default":[]}]}}}' WHERE ATOM_CODE = 'codeTapdWebHookTrigger' AND VERSION = '1.0.0';
COMMIT;
