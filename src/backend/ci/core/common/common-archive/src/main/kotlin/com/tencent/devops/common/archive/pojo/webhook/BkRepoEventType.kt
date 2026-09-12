package com.tencent.devops.common.archive.pojo.webhook

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * bkrepo 事件类型
 *
 * 对应 bkrepo webhook 接口的 triggers 字段，枚举名与 bkrepo EventType 保持一致。
 * 仅收录与制品相关的事件（项目/仓库/节点/元数据/包版本），
 * 其余管理类事件不作为制品触发订阅项。
 *
 * 说明：bkrepo 不存在 NODE_UPDATED 事件，二进制文件覆盖同样上报 NODE_CREATED。
 */
enum class BkRepoEventType {
    // 兜底类型，兼容 bkrepo 新增事件反序列化，避免抛异常
    UNKNOWN,

    // PROJECT
    PROJECT_CREATED,

    // REPOSITORY
    REPO_CREATED,
    REPO_UPDATED,

    // NODE（二进制文件制品）
    NODE_CREATED,
    NODE_RENAMED,
    NODE_MOVED,
    NODE_COPIED,
    NODE_DELETED,
    NODE_DOWNLOADED,

    // METADATA
    METADATA_SAVED,
    METADATA_DELETED,

    // VERSION（容器镜像 / 包制品）
    VERSION_CREATED;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): BkRepoEventType =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
