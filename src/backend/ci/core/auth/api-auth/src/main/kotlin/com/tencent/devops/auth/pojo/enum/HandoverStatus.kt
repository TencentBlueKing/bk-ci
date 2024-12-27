package com.tencent.devops.auth.pojo.enum

enum class HandoverStatus(val value: Int) {
    // 审批中
    PENDING(0),

    // 审批成功
    SUCCEED(1),

    // 审批驳回
    REJECT(2),

    // 撤销
    REVOKE(3);

    companion object {
        fun get(value: Int): HandoverStatus {
            HandoverStatus.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
