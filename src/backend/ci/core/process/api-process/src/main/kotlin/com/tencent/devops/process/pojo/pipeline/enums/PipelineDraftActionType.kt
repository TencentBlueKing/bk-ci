package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线草稿操作类型")
enum class PipelineDraftActionType {
    @Schema(description = "编辑")
    EDIT,

    @Schema(description = "保存")
    SAVE,

    @Schema(description = "发布")
    RELEASE
}
