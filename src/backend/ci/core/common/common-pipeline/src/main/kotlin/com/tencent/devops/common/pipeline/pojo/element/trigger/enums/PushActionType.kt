package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

enum class PushActionType(
    val value: String
) {
    CLIENT_PUSH("clientpush"),
    CREATE_BRANCH("createbranch")
}
