package com.tencent.devops.auth.pojo.enum

enum class HandoverAction(val value: Int) {
    // 审批成功
    AGREE(1),

    // 审批驳回
    REJECT(2),

    // 撤销
    REVOKE(3);

    companion object {
        fun get(value: Int): HandoverAction {
            HandoverAction.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
