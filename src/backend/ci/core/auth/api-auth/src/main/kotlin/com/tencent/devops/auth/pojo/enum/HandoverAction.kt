package com.tencent.devops.auth.pojo.enum

enum class HandoverAction(
    val value: Int,
    val alias: String,
    val emailContent: String,
    val weworkContent: String
) {
    // 审批成功
    AGREE(
        1,
        "已通过",
        "你提交的权限交接单 %s 已被 %s 通过。恭喜你完成交接。",
        "你提交的权限交接单 %s 已被 %s 通过。恭喜你完成交接。"
    ),

    // 审批驳回
    REJECT(
        2,
        "已拒绝",
        "你提交的权限交接单 %s 已被 %s <font color=\"#FF0000\">拒绝</font>。请重新交接。",
        "你提交的权限交接单 %s 已被 %s 拒绝。请重新交接。"
    ),

    // 撤销
    REVOKE(
        3,
        "撤销",
        "已撤销",
        "已撤销"
    );

    companion object {
        fun get(value: Int): HandoverAction {
            HandoverAction.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
