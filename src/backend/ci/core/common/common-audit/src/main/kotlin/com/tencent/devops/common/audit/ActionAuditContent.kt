package com.tencent.devops.common.audit

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID
import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME

@Suppress("MaxLineLength")
object ActionAuditContent {
    private const val CONTENT_TEMPLATE = "[{{$INSTANCE_NAME}}]({{$INSTANCE_ID}})"
    private const val PROJECT_CODE_CONTENT_TEMPLATE = "[{{@PROJECT_CODE}}]"
    const val PROJECT_CODE_TEMPLATE = "@PROJECT_CODE"
    const val PROJECT_ENABLE_OR_DISABLE_TEMPLATE = "@ENABLE"
    const val PROJECT_ADD_OR_REMOVE_TEMPLATE = "@ADD"
    const val BUILD_ID_TEMPLATE = "@BUILD_ID"
    const val ASSIGNS_TEMPLATE = "@ASSIGNS"

    // 项目
    const val PROJECT_MANAGE_RESTORE_PIPELINE_CONTENT = "restore pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_CONTENT = "manage project $CONTENT_TEMPLATE"
    const val PROJECT_CREATE_CONTENT = "create project $CONTENT_TEMPLATE"
    const val PROJECT_EDIT_CONTENT = "modify project $CONTENT_TEMPLATE"
    const val PROJECT_ENABLE_CONTENT = "{{$PROJECT_ENABLE_OR_DISABLE_TEMPLATE}} project $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_ARCHIVED_PIPELINE_CONTENT = "manage project archived pipeline $CONTENT_TEMPLATE"

    // 流水线
    const val PIPELINE_VIEW_CONTENT = "get pipeline info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_SHARE_CONTENT = "share pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_CREATE_CONTENT = "create pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_LIST_CONTENT = "list pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_DOWNLOAD_CONTENT = "download pipeline $CONTENT_TEMPLATE buildId [{{$BUILD_ID_TEMPLATE}}] " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_CONTENT = "update pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_SAVE_SETTING_CONTENT = "save pipeline setting $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_EXPORT_PIPELINE_CONTENT = "export pipeline $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_BIND_PIPELINE_CALLBACK_CONTENT = "bind pipeline call back $CONTENT_TEMPLATE" +
        " in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_DELETE_CONTENT = "delete pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_DELETE_VERSION_CONTENT = "delete pipeline version $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_EXECUTE_CONTENT = "execute pipeline $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 流水线组
    const val PIPELINE_GROUP_CREATE_CONTENT = "create pipeline group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_GROUP_EDIT_CONTENT = "modify pipeline group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_GROUP_VIEW_CONTENT = "get pipeline group info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_GROUP_DELETE_CONTENT = "delete pipeline group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_GROUP_ADD_REMOVE_CONTENT = "{{$PROJECT_ADD_OR_REMOVE_TEMPLATE}} pipeline group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_GROUP_MANAGE_CONTENT = "manage pipeline group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 流水线模板
    const val PIPELINE_TEMPLATE_CREATE_CONTENT = "create template $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_COPY_CONTENT = "copy template $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_SAVE_AS_CONTENT = "save as template $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_DELETE_CONTENT = "delete template $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_SETTING_CONTENT = "update template setting $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_CONTENT = "update template $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 证书
    const val CERT_CREATE_CONTENT = "create cert $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CERT_VIEW_CONTENT = "get cert info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CERT_EDIT_CONTENT = "update cert $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CERT_DELETE_CONTENT = "delete cert $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CERT_LIST_CONTENT = "list cert $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CERT_USE_CONTENT = "use cert $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 凭据
    const val CREDENTIAL_CREATE_CONTENT = "create credential $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_VIEW_CONTENT = "get credential info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_EDIT_CONTENT = "update credential $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_EDIT_SETTING_CONTENT = "update credential setting $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_DELETE_CONTENT = "delete credential $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_LIST_CONTENT = "list credential $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CREDENTIAL_USE_CONTENT = "use credential $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 环境
    const val ENVIRONMENT_CREATE_CONTENT = "create environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_EDIT_CONTENT = "modify environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_EDIT_ADD_NODES_CONTENT = "add environment nodes $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_EDIT_SET_SHARE_ENV_CONTENT = "set share environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_EDIT_DELETE_NODES_CONTENT = "delete environment  nodes $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_VIEW_CONTENT = "get environment info  $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_DELETE_CONTENT = "delete environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_OF_SHARE_DELETE_CONTENT = "delete share environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_LIST_CONTENT = "list environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_USE_CONTENT = "use environment $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENVIRONMENT_ENABLE_OR_DISABLE_NODE = "{{$PROJECT_ENABLE_OR_DISABLE_TEMPLATE}} env node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 环境节点
    const val ENV_NODE_CREATE_CONTENT = "create environment node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_EDIT_CONTENT = "modify environment node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_VIEW_CONTENT = "get environment node info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_DELETE_CONTENT = "delete environment node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_LIST_CONTENT = "list environment node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_USE_CONTENT = "use environment node $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 环境节点标签
    const val ENV_NODE_TAG_CREATE_CONTENT = "create environment tag $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_TAG_EDIT_CONTENT = "edit environment tag $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_TAG_DELETE_CONTENT = "delete environment tag $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val ENV_NODE_TAG_UPDATE_CONTENT = "update environment tag $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 质量红线
    const val RULE_CREATE_CONTENT = "create rule $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val RULE_DELETE_CONTENT = "delete rule $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val RULE_EDIT_CONTENT = "modify rule $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val RULE_ENABLE_CONTENT = "enable/disable rule $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val QUALITY_GROUP_CREATE_CONTENT = "create quality group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val QUALITY_GROUP_DELETE_CONTENT = "delete quality group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val QUALITY_GROUP_EDIT_CONTENT = "modify quality group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 代码库
    const val REPERTORY_CREATE_CONTENT = "create repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_VIEW_CONTENT = "get repertory info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_EDIT_CONTENT = "modify repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_EDIT_RENAME_CONTENT = "rename repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_EDIT_LOCK_CONTENT = "lock/unlock repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_DELETE_CONTENT = "delete repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val REPERTORY_USE_CONTENT = "use repertory $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 版本体验
    const val EXPERIENCE_TASK_CREATE_CONTENT = "create experience task $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_TASK_VIEW_CONTENT = "get experience task info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_TASK_EDIT_CONTENT = "modify experience task $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_TASK_DELETE_CONTENT = "online/offline experience task $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_GROUP_CREATE_CONTENT = "create experience group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_GROUP_VIEW_CONTENT = "get experience group info $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_GROUP_EDIT_CONTENT = "modify experience group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val EXPERIENCE_GROUP_DELETE_CONTENT = "delete experience group $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 自定义
    const val PROJECT_USER_VERIFY_CONTENT = "verify project user $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val WATER_MARK_GET_CONTENT = "get user water mark $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
}
