package com.tencent.devops.store.pojo.event

import com.tencent.devops.store.pojo.event.enums.MappingSource
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 事件字段映射
 */
data class EventFieldMappingItem(
    @get:Schema(title = "源信息路径")
    val sourcePath: String,
    @get:Schema(title = "目标字段")
    val targetField: String,
    @get:Schema(title = "信息来源")
    val source: MappingSource
)
