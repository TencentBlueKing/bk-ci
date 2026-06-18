package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.constant.AuthI18nConstants

enum class BatchAddSkipReason(
    val messageCode: String,
    val defaultMessage: String
) {
    USER_NOT_FOUND_OR_DEPARTED(
        messageCode = AuthI18nConstants.BK_BATCH_ADD_MEMBERS_SKIP_USER_NOT_FOUND_OR_DEPARTED,
        defaultMessage = "以下用户不存在或已离职，未添加"
    ),
    ALREADY_IN_GROUP(
        messageCode = AuthI18nConstants.BK_BATCH_ADD_MEMBERS_SKIP_USER_ALREADY_IN_GROUP,
        defaultMessage = "以下用户已在当前组中，无需重复添加"
    ),
    ALREADY_IN_GROUP_BY_DEPARTMENT(
        messageCode = AuthI18nConstants.BK_BATCH_ADD_MEMBERS_SKIP_USER_ALREADY_IN_GROUP_BY_DEPARTMENT,
        defaultMessage = "以下用户已通过所在组织加入当前组，无需重复添加"
    )
}
