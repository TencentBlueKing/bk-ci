package com.tencent.devops.common.audit

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID
import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME

@Suppress("MaxLineLength")
object TencentActionAuditContent {
    private const val CONTENT_TEMPLATE = "[{{$INSTANCE_NAME}}]({{$INSTANCE_ID}})"
    private const val PROJECT_CODE_CONTENT_TEMPLATE = "[{{@PROJECT_CODE}}]"
    const val PROJECT_CODE_TEMPLATE = "@PROJECT_CODE"
    const val PROJECT_ENABLE_OR_DISABLE_TEMPLATE = "@ENABLE"
    const val PROJECT_ADD_OR_REMOVE_TEMPLATE = "@ADD"
    const val BUILD_ID_TEMPLATE = "@BUILD_ID"
    const val ASSIGNS_TEMPLATE = "@ASSIGNS"

    // 云桌面
    const val CGS_CREATE_CONTENT = "create workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_LIST_CONTENT = "list workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_VIEW_CONTENT = "get workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_STOP_CONTENT = "stop workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_START_CONTENT = "start workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_RESTART_CONTENT = "restart workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_ASSIGN_USER_CONTENT = "assign workspace $CONTENT_TEMPLATE " +
        "to [{{$ASSIGNS_TEMPLATE}}] from $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_ASSIGN_PROJECT_CONTENT = "assign workspace $CONTENT_TEMPLATE to project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_EDIT_TYPE_CONTENT = "modify workspace type $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_REBUILD_SYSTEM_DISK_CONTENT = "rebuild workspace system disk $CONTENT_TEMPLATE " +
        "in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_MAKE_IMAGE_CONTENT = "make workspace image  $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_EXPAND_DISK_CONTENT = "expand workspace disk $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_DELETE_CONTENT = "delete workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_SHARE_CONTENT = "share workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_EDIT_CONTENT = "edit workspace $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val CGS_TOKEN_GENERATE_CONTENT = "generate workspace 1password $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 云桌面镜像
    const val IMAGE_LIST_CONTENT = "list workspace image $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val IMAGE_DELETE_CONTENT = "delete workspace image $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val IMAGE_EDIT_CONTENT = "modify workspace image $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"

    // 代理仓库
    const val TGIT_LINK_CREATE_CONTENT = "create tgit link $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val TGIT_LINK_CALLBACK_CREATE_CONTENT = "create tgit link callback $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val TGIT_LINK_DELETE_CONTENT = "delete tgit link $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
    const val TGIT_LINK_CREATE_PROJECT_CONTENT = "create tgit project $CONTENT_TEMPLATE in project $PROJECT_CODE_CONTENT_TEMPLATE"
}
