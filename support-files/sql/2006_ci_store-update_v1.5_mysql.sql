USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
	
	IF EXISTS(SELECT 1
                  FROM `T_ATOM`
                  WHERE `ATOM_CODE` = 'codeGitlabWebHookTrigger') THEN
        UPDATE `T_ATOM` SET `PROPS`='{"repositoryType":{"rule":{},"type":"enum","component":"enum-input","required":true,"label":"代码库","list":[{"value":"ID","label":"按代码库选择"},{"value":"NAME","label":"按代码库别名输入"}],"default":"ID","desc":""},"repositoryHashId":{"rule":{},"type":"text","label":"代码库","hasAddItem":true,"required":true,"component":"request-selector","searchable":true,"placeholder":"请选择代码库名称","default":"","url":"/repository/api/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=CODE_GITLAB&page=1&pageSize=100","paramId":"repositoryHashId","paramName":"aliasName","tools":{"edit":true,"del":false}},"repositoryName":{"rule":{},"type":"text","component":"vuex-input","required":true,"hidden":true,"label":"","placeholder":"请输入代码库别名","default":""},"branchName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"分支名称","placeholder":"默认为所有分支","default":""},"excludeBranchName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"排除以下目标分支","placeholder":"多个分支间以英文逗号分隔","default":"","rely":{"operation":"NOT","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"includeSourceBranchName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"监听以下源分支","placeholder":"多个路径间以英文逗号分隔","default":"","rely":{"operation":"OR","expression":[{"key":"eventType","value":"MERGE_REQUEST"},{"key":"eventType","value":"MERGE_REQUEST_ACCEPT"}]}},"excludeSourceBranchName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"排除以下源分支","placeholder":"多个路径间以英文逗号分隔","default":"","rely":{"operation":"OR","expression":[{"key":"eventType","value":"MERGE_REQUEST"},{"key":"eventType","value":"MERGE_REQUEST_ACCEPT"}]}},"tagName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"监听以下tag","placeholder":"默认为所有tag，多个tag间以英文逗号分隔","default":"","rely":{"operation":"AND","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"excludeTagName":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"排除以下tag","placeholder":"多个tag间以英文逗号分隔","default":"","rely":{"operation":"AND","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"includePaths":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"监听以下路径","placeholder":"多个路径间以英文逗号分隔","default":"","rely":{"operation":"NOT","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"excludePaths":{"rule":{},"type":"text","required":false,"component":"vuex-input","label":"排除以下路径","placeholder":"多个路径间以英文逗号分隔","default":"","rely":{"operation":"NOT","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"excludeUsers":{"rule":{},"component":"staff-input","required":false,"multiSelect":true,"label":"排除以下人员","default":[],"rely":{"operation":"NOT","expression":[{"key":"eventType","value":"TAG_PUSH"}]}},"eventType":{"rule":{},"type":"text","required":false,"component":"enum-input","label":"事件类型","list":[{"label":"Commit Push Hook","value":"PUSH","tips":"有新的推送事件到项目时，会触发。 如果是tags推送，则不会触发。"},{"label":"Tag Push Hook","value":"TAG_PUSH","tips":"项目中新增或删除tags时会触发"},{"label":"Merge Request Hook","value":"MERGE_REQUEST","tips":"项目中新增Merge Request时会触发"},{"label":"Merge Request Accept Hook","value":"MERGE_REQUEST_ACCEPT","tips":"项目中Merge Request accept时会触发"}],"default":"PUSH"}}' where `ATOM_CODE`='codeGitlabWebHookTrigger';
	END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
