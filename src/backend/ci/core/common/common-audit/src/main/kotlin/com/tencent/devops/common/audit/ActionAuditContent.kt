package com.tencent.devops.common.audit

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID
import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME

object ActionAuditContent {
    // 流水线
    private const val CONTENT_TEMPLATE = "[{{$INSTANCE_NAME}}]({{$INSTANCE_ID}})"
    const val PIPELINE_VIEW_CONTENT = "get pipeline info $CONTENT_TEMPLATE"
    const val PIPELINE_SHARE_CONTENT = "share pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_CREATE_CONTENT = "create pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_LIST_CONTENT = "list pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_DOWNLOAD_CONTENT = "download pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_EDIT_CONTENT = "update pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_DELETE_CONTENT = "delete pipeline $CONTENT_TEMPLATE"
    const val PIPELINE_EXECUTE_CONTENT = "execute pipeline $CONTENT_TEMPLATE"
}
