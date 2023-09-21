package com.tencent.devops.common.audit

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID
import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME

object ActionAuditContent {
    private const val CONTENT_TEMPLATE = "[{{$INSTANCE_NAME}}]({{$INSTANCE_ID}})"

    // 项目
    const val PROJECT_MANAGE_RESTORE_PIPELINE_CONTENT = "restore pipeline $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_CREATE_TEMPLATE_CONTENT = "create template $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_COPY_TEMPLATE_CONTENT = "copy template $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_SAVE_AS_TEMPLATE_CONTENT = "save as template $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_DELETE_TEMPLATE_CONTENT = "delete template $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_UPDATE_TEMPLATE_SETTING_CONTENT = "update template setting $CONTENT_TEMPLATE"
    const val PROJECT_MANAGE_UPDATE_TEMPLATE_CONTENT = "update template $CONTENT_TEMPLATE"

    // 流水线
    const val PIPELINE_VIEW_CONTENT = "get pipeline info $CONTENT_TEMPLATE"
    const val PIPELINE_SHARE_CONTENT = "share pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_CREATE_CONTENT = "create pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_LIST_CONTENT = "list pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_DOWNLOAD_CONTENT = "download pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_CONTENT = "update pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_SAVE_SETTING_CONTENT = "save pipeline setting $CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_EXPORT_PIPELINE_CONTENT = "export pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_BIND_PIPELINE_CALLBACK_CONTENT = "bind pipeline call back $CONTENT_TEMPLATE"
    const val PIPELINE_DELETE_CONTENT = "delete pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_DELETE_VERSION_CONTENT = "delete pipeline version $CONTENT_TEMPLATE"
    const val PIPELINE_EXECUTE_CONTENT = "execute pipeline $CONTENT_TEMPLATE"

    // 证书
    const val CERT_CREATE_CONTENT = "create cert $CONTENT_TEMPLATE"
    const val CERT_VIEW_CONTENT = "get cert info $CONTENT_TEMPLATE"
    const val CERT_EDIT_CONTENT = "update cert $CONTENT_TEMPLATE"
    const val CERT_DELETE_CONTENT = "delete cert $CONTENT_TEMPLATE"
    const val CERT_LIST_CONTENT = "list cert $CONTENT_TEMPLATE"
    const val CERT_USE_CONTENT = "use cert $CONTENT_TEMPLATE"

    // 凭据
    const val CREDENTIAL_CREATE_CONTENT = "create credential $CONTENT_TEMPLATE"
    const val CREDENTIAL_VIEW_CONTENT = "get credential info $CONTENT_TEMPLATE"
    const val CREDENTIAL_EDIT_CONTENT = "update credential $CONTENT_TEMPLATE"
    const val CREDENTIAL_EDIT_SETTING_CONTENT = "update credential setting $CONTENT_TEMPLATE"
    const val CREDENTIAL_DELETE_CONTENT = "delete credential $CONTENT_TEMPLATE"
    const val CREDENTIAL_LIST_CONTENT = "list credential $CONTENT_TEMPLATE"
    const val CREDENTIAL_USE_CONTENT = "use credential $CONTENT_TEMPLATE"
}
