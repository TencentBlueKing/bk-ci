package com.tencent.devops.auth.provider.rbac.service

enum class BatchAddUserResult(val reason: BatchAddSkipReason?) {
    ADD(reason = null),
    ALREADY_IN_GROUP(reason = BatchAddSkipReason.ALREADY_IN_GROUP),
    ALREADY_IN_GROUP_BY_DEPARTMENT(reason = BatchAddSkipReason.ALREADY_IN_GROUP_BY_DEPARTMENT),
    USER_NOT_FOUND_OR_DEPARTED(reason = BatchAddSkipReason.USER_NOT_FOUND_OR_DEPARTED)
}
