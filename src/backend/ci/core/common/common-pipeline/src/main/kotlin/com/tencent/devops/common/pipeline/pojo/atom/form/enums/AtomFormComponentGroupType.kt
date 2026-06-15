package com.tencent.devops.common.pipeline.pojo.atom.form.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件表单分组类型")
enum class AtomFormComponentGroupType {
    @Schema(title = "触发器组件分组")
    TRIGGER_GROUP,

    @Schema(title = "研发商店组件规范分组, 使用inputGroups")
    STORE_GROUP;
}