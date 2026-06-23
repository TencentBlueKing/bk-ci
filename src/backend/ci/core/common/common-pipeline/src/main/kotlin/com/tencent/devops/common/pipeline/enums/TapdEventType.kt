package com.tencent.devops.common.pipeline.enums

/**
 * TAPD 事件类型
 */
enum class TapdEventType(val value: String) {
    // 工单类
    STORY("story"),
    BUG("bug"),
    TASK("task"),
    TOBJECT("tobject"),

    // 项目流程
    LAUNCHFORM("launchform"),
    RELEASE("release"),
    ITERATION("iteration"),
    VERSION("version"),

    // 看板
    BOARD("board"),
    BOARD_CARD("board_card"),
    KANBAN("kanban"),

    // 关联
    BRANCH("branch"),
    COMMIT("commit"),

    // 评论
    STORY_COMMENT("story_comment"),
    BUG_COMMENT("bug_comment"),
    TASK_COMMENT("task_comment"),

    // 附件
    STORY_ATTACHMENT("story_attachment"),
    BUG_ATTACHMENT("bug_attachment"),
    TASK_ATTACHMENT("task_attachment"),
    OPEN_APP_ATTACHMENT("open_app_attachment"),

    // 开放应用
    OPEN_APP("open_app"),

    // 角色
    ROLE("role"),

    // 测试
    TEST_PLAN("test_plan");

    companion object {
        fun parse(value: String?): TapdEventType? {
            if (value.isNullOrBlank()) return null
            return values().firstOrNull { it.value.equals(value, ignoreCase = true) }
        }
    }
}
