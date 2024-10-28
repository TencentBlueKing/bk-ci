package com.tencent.devops.auth.pojo.enum

enum class HandoverAction(val value: Int, val alias: String) {
    // 审批成功
    AGREE(1, "已通过"),

    // 审批驳回
    REJECT(2, "已被拒绝"),

    // 撤销
    REVOKE(3, "撤销");

    companion object {
        fun get(value: Int): HandoverAction {
            HandoverAction.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
