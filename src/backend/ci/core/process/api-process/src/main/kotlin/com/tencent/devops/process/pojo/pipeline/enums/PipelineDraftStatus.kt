package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线草稿状态")
enum class PipelineDraftStatus {
    @Schema(title = "正常")
    NORMAL,

    @Schema(title = "草稿已存在")
    EXISTS,

    @Schema(title = "存在冲突")
    CONFLICT,

    @Schema(title = "版本落后")
    OUTDATED,

    @Schema(title = "已发布")
    PUBLISHED
}
