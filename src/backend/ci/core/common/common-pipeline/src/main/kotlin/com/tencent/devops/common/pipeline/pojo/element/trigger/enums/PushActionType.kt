package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

enum class PushActionType(
    val value: String
) {
    NEW_BRANCH("new-branch"),
    PUSH_FILE("push-file")
}
