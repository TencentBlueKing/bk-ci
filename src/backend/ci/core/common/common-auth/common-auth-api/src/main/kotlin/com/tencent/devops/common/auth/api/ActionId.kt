package com.tencent.devops.common.auth.api

object ActionId {
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
}
