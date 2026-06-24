package com.tencent.devops.process.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "获取流水线版本来源")
enum class PipelineGetVersionSource {
    @Schema(name = "编辑")
    EDIT,
    @Schema(name = "查看")
    VIEW,
    @Schema(name = "对比")
    COMPARE
}
