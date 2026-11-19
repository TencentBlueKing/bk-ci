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
UPDATE T_ATOM SET PROPS = '{"input":{"tips":{"label":"","type":"tips-simple","tipStr":"TAPD事件触发需提前安装应用，<a target=''_blank'' href=''/ms/repository/api/user/tapd/appInstall''>点击安装</a>"},"workspaceId":{"rule":{},"type":"request-selector","component":"request-selector","required":true,"label":"TAPD项目","placeholder":"请选择TAPD项目","default":"","url":"/repository/api/user/tapd/workspaces","paramId":"id","paramName":"name","searchable":true,"hasAddItem":false},"eventType":{"rule":{},"required":true,"component":"enum-input","type":"enum-input","label":"事件类型","list":[{"label":"需求","value":"STORY"},{"label":"缺陷","value":"BUG"}],"default":"STORY"},"includeEventFrom":{"label":"监听的事件来源","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"api","name":"API"},{"id":"web","name":"页面"},{"id":"auto_task","name":"自动化任务"}],"default":["api","web","auto_task"]},"includeStoryAction":{"label":"监听的动作","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"create","name":"新建"},{"id":"update","name":"更新"},{"id":"delete","name":"删除"},{"id":"status_change","name":"状态变更"},{"id":"bug_link","name":"关联缺陷"},{"id":"bug_unlink","name":"取消关联缺陷"},{"id":"story_link","name":"关联需求"},{"id":"story_unlink","name":"取消关联需求"},{"id":"add_comment","name":"新增评论"},{"id":"update_comment","name":"更新评论"},{"id":"delete_comment","name":"删除评论"}],"default":["create","update","delete"],"rely":{"operation":"AND","expression":[{"key":"eventType","value":"STORY"}]}},"includeBugAction":{"label":"监听的动作","bottomDivider":true,"required":true,"component":"atom-checkbox-list","multiSelect":true,"list":[{"id":"create","name":"新建"},{"id":"update","name":"更新"},{"id":"delete","name":"删除"},{"id":"status_change","name":"状态变更"},{"id":"add_comment","name":"新增评论"},{"id":"update_comment","name":"更新评论"},{"id":"delete_comment","name":"删除评论"}],"default":["create","update","delete"],"rely":{"operation":"AND","expression":[{"key":"eventType","value":"BUG"}]}},"includePriority":{"rule":{},"type":"vuex-input","required":false,"label":"监听以下优先级","desc":"","placeholder":"多个优先级用英文逗号分隔","default":""},"userSettings":{"type":"group","label":"用户设置","children":[{"key":"includeUsers","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"包含以下用户","desc":"如需包含公共账号，可将对应账号定义为流水线变量（多个用户用英文逗号分隔），然后通过 ${{ variable }} 引用","default":[]},{"key":"excludeUsers","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"排除以下用户","desc":"如需排除公共账号，可将对应账号定义为流水线变量（多个用户用英文逗号分隔），然后通过 ${{ variable }} 引用","default":[]}]},"labelSettings":{"type":"group","label":"标签设置","children":[{"key":"includeLabels","type":"groupItem","rule":{},"component":"composite-input","required":false,"multiSelect":true,"prependText":"包含以下标签","desc":"多个标签用英文逗号分隔","default":""},{"key":"excludeLabels","type":"groupItem","rule":{},"component":"composite-input","required":false,"multiSelect":true,"prependText":"排除以下标签","desc":"多个标签用英文逗号分隔","default":""}]},"ownerSettings":{"type":"group","label":"处理人设置","children":[{"key":"includeOwner","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"包含以下处理人","desc":"如需包含公共账号，可将对应账号定义为流水线变量（多个用户用英文逗号分隔），然后通过 ${{ variable }} 引用","default":[]},{"key":"excludeOwner","type":"groupItem","rule":{},"component":"staff-input","required":false,"multiSelect":true,"prependText":"排除以下处理人","desc":"如需排除公共账号，可将对应账号定义为流水线变量（多个用户用英文逗号分隔），然后通过 ${{ variable }} 引用","default":[]}]},"statusSettings":{"type":"group","label":"状态设置","children":[{"key":"includeStatuses","type":"groupItem","component":"devops-select","required":false,"default":[],"label":"监听以下状态","placeholder":"留空表示监听所有状态","rely":{"operation":"NOT","expression":[{"key":"workspaceId","value":""}]},"optionsConf":{"url":"/repository/api/user/tapd/statuses?workspaceId={workspaceId}&eventType={eventType}","paramId":"id","paramName":"name","searchable":true,"multiple":true}},{"key":"excludeStatuses","type":"groupItem","component":"devops-select","required":false,"default":[],"label":"排除以下状态","placeholder":"命中时不触发流水线","rely":{"operation":"NOT","expression":[{"key":"workspaceId","value":""}]},"optionsConf":{"url":"/repository/api/user/tapd/statuses?workspaceId={workspaceId}&eventType={eventType}","paramId":"id","paramName":"name","searchable":true,"multiple":true}}]}}}' WHERE ATOM_CODE = 'codeTapdWebHookTrigger' AND VERSION = '1.0.0';
COMMIT;
