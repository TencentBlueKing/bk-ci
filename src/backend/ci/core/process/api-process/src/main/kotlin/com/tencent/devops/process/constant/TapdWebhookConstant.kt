package com.tencent.devops.process.constant

/**
 * TAPD webhook相关常量
 */
object TapdWebhookConstant {
    // 事件标识字段名，例如 story::create / bug::update
    const val TAPD_KEY_EVENT = "event"

    // 事件分隔符
    const val TAPD_EVENT_SEPARATOR = "::"

    // 项目ID字段名（workspace_id）
    const val TAPD_KEY_WORKSPACE_ID = "workspace_id"

    // 触发用户字段名
    const val TAPD_KEY_CURRENT_USER = "current_user"

    const val TAPD_KEY_NEW_PREFIX = "new"

    // 工单ID字段名（创建/删除事件用）
    const val TAPD_KEY_ID = "id"

    // 工单标题字段
    const val TAPD_KEY_NAME = "name"

    const val TAPD_KEY_PRIORITY = "priority"

    const val TAPD_KEY_PARENT_ID = "parent_id"

    const val TAPD_KEY_EVENT_FROM = "event_from"

    // 用于从中提取 TAPD 主机地址（schema + host）
    const val TAPD_KEY_REFERER = "referer"

    // 需求详情：{tapdHost}/tapd_fe/{workspaceId}/story/detail/{id}
    const val TAPD_STORY_URL_PATTERN = "%s/tapd_fe/%s/story/detail/%s"

    // 缺陷详情：{tapdHost}/tapd_fe/{workspaceId}/bug/detail/{id}
    const val TAPD_BUG_URL_PATTERN = "%s/tapd_fe/%s/bug/detail/%s"
}
