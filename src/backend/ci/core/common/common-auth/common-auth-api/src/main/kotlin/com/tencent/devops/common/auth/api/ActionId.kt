package com.tencent.devops.common.auth.api

object ActionId {
    // 项目
    const val PROJECT_VISIT = "project_visit"
    const val PROJECT_CREATE = "project_create"
    const val PROJECT_EDIT = "project_edit"
    const val PROJECT_ENABLE = "project_enable"
    const val PROJECT_MANAGE = "project_manage"
    const val PROJECT_MANAGE_ARCHIVED_PIPELINE = "project_manage-archived-pipeline"

    // 流水线
    const val PIPELINE_CREATE = "pipeline_create"
    const val PIPELINE_VIEW = "pipeline_view"
    const val PIPELINE_EDIT = "pipeline_edit"
    const val PIPELINE_DELETE = "pipeline_delete"
    const val PIPELINE_EXECUTE = "pipeline_execute"
    const val PIPELINE_LIST = "pipeline_list"
    const val PIPELINE_SHARE = "pipeline_share"
    const val PIPELINE_DOWNLOAD = "pipeline_download"

    // 流水线组
    const val PIPELINE_GROUP_CREATE = "pipeline_group_create"
    const val PIPELINE_GROUP_EDIT = "pipeline_group_edit"
    const val PIPELINE_GROUP_VIEW = "pipeline_group_view"
    const val PIPELINE_GROUP_DELETE = "pipeline_group_delete"
    const val PIPELINE_GROUP_ADD_REMOVE = "pipeline_group_add_remove"
    const val PIPELINE_GROUP_MANAGE = "pipeline_group_manage"

    // 流水线模板
    const val PIPELINE_TEMPLATE_CREATE = "pipeline_template_create"
    const val PIPELINE_TEMPLATE_EDIT = "pipeline_template_edit"
    const val PIPELINE_TEMPLATE_DELETE = "pipeline_template_delete"

    // 凭据
    const val CREDENTIAL_CREATE = "credential_create"
    const val CREDENTIAL_VIEW = "credential_view"
    const val CREDENTIAL_EDIT = "credential_edit"
    const val CREDENTIAL_DELETE = "credential_delete"
    const val CREDENTIAL_LIST = "credential_list"
    const val CREDENTIAL_USE = "credential_use"

    // 证书
    const val CERT_CREATE = "cert_create"
    const val CERT_VIEW = "cert_view"
    const val CERT_EDIT = "cert_edit"
    const val CERT_DELETE = "cert_delete"
    const val CERT_LIST = "cert_list"
    const val CERT_USE = "cert_use"

    // 云桌面
    const val CGS_CREATE = "cgs_create"
    const val CGS_LIST = "cgs_list"
    const val CGS_VIEW = "cgs_view"
    const val CGS_STOP = "cgs_stop"
    const val CGS_START = "cgs_start"
    const val CGS_RESTART = "cgs_restart"
    const val CGS_ASSIGN = "cgs_assign"
    const val CGS_EDIT_TYPE = "cgs_edit-type"
    const val CGS_REBUILD_SYSTEM_DISK = "cgs_rebuild-system-disk"
    const val CGS_MAKE_IMAGE = "cgs_make-image"
    const val CGS_EXPAND_DISK = "cgs_expand-disk"
    const val CGS_DELETE = "cgs_delete"
    const val CGS_SHARE = "cgs_share"
    const val CGS_EDIT = "cgs_edit"
    const val CGS_TOKEN_GENERATE = "cgs_token_generate"

    // 镜像
    const val IMAGE_LIST = "image_list"
    const val IMAGE_DELETE = "image_delete"
    const val IMAGE_EDIT = "image_edit"

    // 代理仓库
    const val TGIT_LINK_CREATE = "tgit_link_create"
    const val TGIT_LINK_LIST = "tgit_link_list"
    const val TGIT_LINK_DELETE = "tgit_link_delete"

    // 环境
    const val ENVIRONMENT_CREATE = "environment_create"
    const val ENVIRONMENT_EDIT = "environment_edit"
    const val ENVIRONMENT_VIEW = "environment_view"
    const val ENVIRONMENT_DELETE = "environment_delete"
    const val ENVIRONMENT_LIST = "environment_list"
    const val ENVIRONMENT_USE = "environment_use"

    // 环境节点
    const val ENV_NODE_CREATE = "env_node_create"
    const val ENV_NODE_EDIT = "env_node_edit"
    const val ENV_NODE_VIEW = "env_node_view"
    const val ENV_NODE_DELETE = "env_node_delete"
    const val ENV_NODE_LIST = "env_node_list"
    const val ENV_NODE_USE = "env_node_use"

    // 质量红线
    const val RULE_CREATE = "rule_create"
    const val RULE_DELETE = "rule_delete"
    const val RULE_EDIT = "rule_edit"
    const val RULE_ENABLE = "rule_enable"
    const val QUALITY_GROUP_CREATE = "quality_group_create"
    const val QUALITY_GROUP_DELETE = "quality_group_delete"
    const val QUALITY_GROUP_EDIT = "quality_group_edit"

    // 代码库
    const val REPERTORY_CREATE = "repertory_create"
    const val REPERTORY_VIEW = "repertory_view"
    const val REPERTORY_EDIT = "repertory_edit"
    const val REPERTORY_DELETE = "repertory_delete"
    const val REPERTORY_USE = "repertory_use"

    // 版本体验
    const val EXPERIENCE_TASK_CREATE = "experience_task_create"
    const val EXPERIENCE_TASK_VIEW = "experience_task_view"
    const val EXPERIENCE_TASK_EDIT = "experience_task_edit"
    const val EXPERIENCE_TASK_DELETE = "experience_task_delete"
    const val EXPERIENCE_GROUP_CREATE = "experience_group_create"
    const val EXPERIENCE_GROUP_VIEW = "experience_group_view"
    const val EXPERIENCE_GROUP_EDIT = "experience_group_edit"
    const val EXPERIENCE_GROUP_DELETE = "experience_group_delete"

    // 自定义操作
    const val PROJECT_USER_VERIFY = "project_user_verify"
    const val WATER_MARK_GET = "water_mark_get"
}
