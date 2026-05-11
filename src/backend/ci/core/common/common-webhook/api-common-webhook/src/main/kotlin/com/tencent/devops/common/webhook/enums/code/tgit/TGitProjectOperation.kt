package com.tencent.devops.common.webhook.enums.code.tgit

import com.fasterxml.jackson.annotation.JsonValue

/**
 * 项目操作类型枚举
 */
enum class TGitProjectOperation(
    @JsonValue
    val value: String
) {
    // 新建项目
    PROJECT_CREATE("project_create"),
    // 删除项目
    PROJECT_DELETE("project_delete"),
    // 引用项目
    PROJECT_REFERENCE("project_reference"),
    // 解除引用项目
    PROJECT_DEREFERENCE("project_dereference"),
    // 转移项目（进）
    PROJECT_TRANSFER_IN("project_transfer_in"),
    // 转移项目（出）
    PROJECT_TRANSFER_OUT("project_transfer_out");

    companion object {
        fun parse(value: String): TGitProjectOperation? {
            return values().firstOrNull { it.value == value }
        }
    }
}
