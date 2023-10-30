package com.tencent.devops.common.audit

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID
import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME

object ActionAuditContent {
    private const val CONTENT_TEMPLATE = "[{{$INSTANCE_NAME}}]({{$INSTANCE_ID}})"

    // 项目
    const val PROJECT_MANAGE_RESTORE_PIPELINE_CONTENT = "restore pipeline $CONTENT_TEMPLATE"

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

    // 流水线模板
    const val PIPELINE_TEMPLATE_CREATE_CONTENT = "create template $CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_COPY_CONTENT = "copy template $CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_SAVE_AS_CONTENT = "save as template $CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_DELETE_CONTENT = "delete template $CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_SETTING_CONTENT = "update template setting $CONTENT_TEMPLATE"
    const val PIPELINE_TEMPLATE_EDIT_CONTENT = "update template $CONTENT_TEMPLATE"

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

    // 云桌面
    const val CGS_CREATE_CONTENT = "create work space $CONTENT_TEMPLATE"
    const val CGS_LIST_CONTENT = "list work space $CONTENT_TEMPLATE"
    const val CGS_VIEW_CONTENT = "get work space $CONTENT_TEMPLATE"
    const val CGS_STOP_CONTENT = "stop work space $CONTENT_TEMPLATE"
    const val CGS_START_CONTENT = "start work space $CONTENT_TEMPLATE"
    const val CGS_RESTART_CONTENT = "restart work space $CONTENT_TEMPLATE"
    const val CGS_ASSIGN_CONTENT = "assign work space $CONTENT_TEMPLATE"
    const val CGS_EDIT_TYPE_CONTENT = "modify work space type $CONTENT_TEMPLATE"
    const val CGS_REBUILD_SYSTEM_DISK_CONTENT = "rebuild work space system disk $CONTENT_TEMPLATE"
    const val CGS_MAKE_IMAGE_CONTENT = "make work space image  $CONTENT_TEMPLATE"
    const val CGS_EXPAND_DISK_CONTENT = "expand work space disk $CONTENT_TEMPLATE"
    const val CGS_DELETE_CONTENT = "delete work space $CONTENT_TEMPLATE"
    const val CGS_SHARE_CONTENT = "share work space $CONTENT_TEMPLATE"
    const val CGS_EDIT_CONTENT = "edit work space $CONTENT_TEMPLATE"

    // 云桌面镜像
    const val IMAGE_LIST_CONTENT = "list work space image $CONTENT_TEMPLATE"
    const val IMAGE_DELETE_CONTENT = "delete work space image $CONTENT_TEMPLATE"
    const val IMAGE_EDIT_CONTENT = "modify work space image $CONTENT_TEMPLATE"

    // 代理仓库
    const val CODE_PROXY_CREATE_CONTENT = "create code proxy $CONTENT_TEMPLATE"
    const val CODE_PROXY_LIST_CONTENT = "list code proxy $CONTENT_TEMPLATE"
    const val CODE_PROXY_DELETE_CONTENT = "delete code proxy $CONTENT_TEMPLATE"
}
