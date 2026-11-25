package com.tencent.devops.process.constant

/**
 * TAPD webhook相关常量
 */
object TapdWebhookConstant {
    // webhook 密钥字段名（TAPD 侧配置后会随 body 一起下发）
    const val TAPD_KEY_SECRET = "secret"

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

    const val TAPD_KEY_ENTITY_ID = "entity_id"

    // 目标对象ID
    const val TAPD_KEY_TARGET_ID = "target_id"

    // 源对象ID
    const val TAPD_KEY_SOURCE_ID = "source_id"

    const val TAPD_KEY_STORY_ID = "story_id"

    const val TAPD_KEY_BUG_ID = "bug_id"

    // 优先级标题
    const val TAPD_KEY_PRIORITY_LABEL = "priority_label"

    const val TAPD_KEY_LABEL = "label"

    // 工单标题字段
    const val TAPD_KEY_NAME = "name"

    // 工单标题字段
    const val TAPD_KEY_TITLE = "title"

    const val TAPD_KEY_OWNER = "owner"

    const val TAPD_KEY_CURRENT_OWNER = "current_owner"

    const val TAPD_KEY_PRIORITY = "priority"

    const val TAPD_KEY_PARENT_ID = "parent_id"

    const val TAPD_KEY_EVENT_FROM = "event_from"

    const val TAPD_KEY_EVENT_ID = "event_id"

    const val TAPD_KEY_CHANGE_FIELDS = "change_fields"

    const val TAPD_KEY_STATUS = "status"

    // 更新事件中的变更后状态字段
    const val TAPD_KEY_NEW_STATUS = "new_status"

    // 派生字段：单据当前英文状态 key（变更后状态优先，创建等事件用 status 兜底）
    const val TAPD_KEY_CURRENT_STATUS = "current_status"

    // 派生字段：单据当前中文状态名（由 TapdWebhookRequestService 实时映射 TAPD 状态接口得到）
    const val TAPD_KEY_CURRENT_STATUS_NAME = "current_status_name"

    // 派生字段：TAPD 状态中英文映射失败标记
    const val TAPD_KEY_STATUS_MAPPING_FAILED = "tapd_status_mapping_failed"

    const val TAPD_KEY_DESCRIPTION = "description"

    // 派生字段：TAPD 项目名称（由 TapdWebhookRequestService 塞入 body，供构建端组装启动参数使用）
    const val TAPD_KEY_WORKSPACE_NAME = "workspace_name"

    // 派生字段：工单详情页链接（由 TapdWebhookRequestService 塞入 body，供构建端组装启动参数使用）
    const val TAPD_KEY_OBJECT_URL = "object_url"

    // 派生字段：事件对象ID（不同事件动作下从不同 body 字段派生，供构建端组装启动参数使用）
    const val TAPD_KEY_OBJECT_ID = "object_id"

    // 需求详情：{tapdHost}/tapd_fe/{workspaceId}/story/detail/{id}
    const val TAPD_STORY_URL_PATTERN = "%s/tapd_fe/%s/story/detail/%s"

    // 缺陷详情：{tapdHost}/tapd_fe/{workspaceId}/bug/detail/{id}
    const val TAPD_BUG_URL_PATTERN = "%s/tapd_fe/%s/bug/detail/%s"
}
