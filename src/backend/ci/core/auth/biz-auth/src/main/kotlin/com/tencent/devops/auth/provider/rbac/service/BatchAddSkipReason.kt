package com.tencent.devops.auth.provider.rbac.service

enum class BatchAddSkipReason(val messagePrefix: String) {
    USER_NOT_FOUND_OR_DEPARTED(messagePrefix = "以下用户不存在或已离职，未添加"),
    ALREADY_IN_GROUP(messagePrefix = "以下用户已在当前组中，无需重复添加"),
    ALREADY_IN_GROUP_BY_DEPARTMENT(messagePrefix = "以下用户已通过所在组织加入当前组，无需重复添加")
}
