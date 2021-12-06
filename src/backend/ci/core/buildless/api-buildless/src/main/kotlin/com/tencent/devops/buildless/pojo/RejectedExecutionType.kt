package com.tencent.devops.buildless.pojo

enum class RejectedExecutionType {
    /*
    可以拒绝
     */
    ABORT_POLICY,

    /*
    不可以拒绝，必须执行
     */
    FOLLOW_POLICY,

    /*
    插队执行
     */
    JUMP_POLICY
}
