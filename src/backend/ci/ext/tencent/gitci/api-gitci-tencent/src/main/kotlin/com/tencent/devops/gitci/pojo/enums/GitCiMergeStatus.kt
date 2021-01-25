package com.tencent.devops.gitci.pojo.enums

enum class GitCiMergeStatus(val value: String) {
    MERGE_STATUS_UNCHECKED("unchecked"),
    MERGE_STATUS_CAN_BE_MERGED("can_be_merged"),
    MERGE_STATUS_CAN_NOT_BE_MERGED("cannot_be_merged"),
    // 项目有配置 mr hook，当创建mr后，发送mr hook前，这个状态是hook_intercept,与gitci无关
    // MERGE_STATUS_HOOK_INTERCEPT("hook_intercept")
}