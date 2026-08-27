package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品触发事件类型（对应产品文档 `on.artifacts.arrived`）。
 *
 * 序列化/存储/订阅落库与匹配统一使用枚举名（大写 ARRIVED），与系统其它枚举保持一致。
 * 当前仅「制品到达」一种。
 */
@Schema(title = "制品触发事件类型")
enum class ArtifactTriggerEventType {
    @Schema(title = "制品到达")
    ARRIVED;
}
