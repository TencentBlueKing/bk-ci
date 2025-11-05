package com.tencent.devops.store.pojo.event

import com.tencent.devops.store.pojo.event.conditions.TriggerCondition
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发事件基础信息")
data class TriggerEventConfig(
    @get:Schema(title = "字段映射")
    val fieldMapping: List<EventFieldMappingItem>,
    @get:Schema(title = "触发条件")
    val conditions: List<TriggerCondition>
)
