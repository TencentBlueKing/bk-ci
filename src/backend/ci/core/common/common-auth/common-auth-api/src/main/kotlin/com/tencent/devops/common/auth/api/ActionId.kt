package com.tencent.devops.common.auth.api

object ActionId {
    // 项目
    const val PROJECT_MANAGE = "project_manage"

    // 流水线
    const val PIPELINE_CREATE = "pipeline_create"
    const val PIPELINE_VIEW = "pipeline_view"
    const val PIPELINE_EDIT = "pipeline_edit"
    const val PIPELINE_DELETE = "pipeline_delete"
    const val PIPELINE_EXECUTE = "pipeline_execute"
    const val PIPELINE_LIST = "pipeline_list"
    const val PIPELINE_SHARE = "pipeline_share"
    const val PIPELINE_DOWNLOAD = "pipeline_download"

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

    // 镜像
    const val IMAGE_LIST = "image_list"
    const val IMAGE_DELETE = "image_delete"
    const val IMAGE_EDIT = "image_edit"

    // 代理仓库
    const val CODE_PROXY_CREATE = "code_proxy_create"
    const val CODE_PROXY_LIST = "code_proxy_list"
    const val CODE_PROXY_DELETE = "code_proxy_delete"
}
