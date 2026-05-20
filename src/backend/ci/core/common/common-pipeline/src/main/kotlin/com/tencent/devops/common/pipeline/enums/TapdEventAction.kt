package com.tencent.devops.common.pipeline.enums

/**
 * TAPD 事件动作
 */
enum class TapdEventAction(val value: String) {
    // 通用 CRUD
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),

    // 评论 / 附件 / 版本 / 角色 / 测试计划
    ADD("add"),

    // 分支关联
    RELATE("relate"),
    UNRELATE("unrelate"),

    // 提交关联
    LINK("link"),
    UNLINK("unlink"),
    // bug关联
    BUG_LINK("bug_link"),
    BUG_UNLINK("bug_unlink"),
    // story关联
    STORY_LINK("story_link"),
    STORY_UNLINK("story_unlink"),
    // 测试计划用例
    LINK_TCASE("link_tcase"),
    UNLINK_TCASE("unlink_tcase"),

    // 测试计划进度
    PROGRESS_UPDATE("progress_update"),

    // 开放应用授权
    CREATE_AUTH("create_auth"),
    CANCEL_AUTH("cancel_auth"),

    // 开放应用开关
    OPEN("open"),
    CLOSED("closed"),

    // 角色加入/退出
    JOIN("join"),
    QUIT("quit"),

    // 扩展动作, 按照事件类型进行合并，实际不存在
    ADD_COMMENT("add_comment"),
    UPDATE_COMMENT("update_comment"),
    DELETE_COMMENT("delete_comment"),
    STATUS_CHANGE("status_change");

    companion object {
        fun parse(value: String?): TapdEventAction? {
            if (value.isNullOrBlank()) {
                return null
            }
            return values().firstOrNull { it.value.equals(value, ignoreCase = true) }
        }
    }
}
