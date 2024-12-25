package com.tencent.devops.auth.pojo.enum

enum class HandoverAction(
    val value: Int,
    val alias: String,
    val content: String
) {
    // 审批成功
    AGREE(1, "已通过", "您提交的权限交接单 %s 已被 %s 通过。恭喜您完成交接。"),

    // 审批驳回
    REJECT(2, "已拒绝", "您提交的权限交接单 %s 已被 %s 拒绝。请重新交接。"),

    // 撤销
    REVOKE(3, "撤销", "已撤销");

    companion object {
        fun get(value: Int): HandoverAction {
            HandoverAction.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
