package com.devops.process.yaml.v2.enums

enum class StreamTriggerAction

enum class StreamObjectKind(val value: String) {
    PUSH("push"),
    TAG_PUSH("tag_push"),
    MERGE_REQUEST("merge_request"),
    MANUAL("manual"),
    SCHEDULE("schedule"),
    DELETE("delete"),
    OPENAPI("openApi"),
    ISSUE("issue"),
    REVIEW("review"),
    NOTE("note");
}

enum class StreamPushActionType(val value: String) {
    NEW_BRANCH("new-branch"),
    PUSH_FILE("push-file");
}

enum class StreamMrEventAction(val value: String) {
    OPEN("open"),
    CLOSE("close"),
    REOPEN("reopen"),
    PUSH_UPDATE("push-update"),
    MERGE("merge");
}
