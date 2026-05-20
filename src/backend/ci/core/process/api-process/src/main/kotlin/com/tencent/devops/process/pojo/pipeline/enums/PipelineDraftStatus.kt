package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线草稿状态")
enum class PipelineDraftStatus {
    @Schema(title = "正常")
    NORMAL,

    @Schema(title = "草稿已存在", description = "当前操作人和原草稿的保存人相同,但草稿已超过7天")
    EXISTS,

    @Schema(title = "存在冲突", description = "当前操作人和最新草稿的保存人不相同")
    CONFLICT,

    @Schema(title = "草稿基线版本落后", description = "草稿基线版本早于当前最新版本")
    OUTDATED,

    @Schema(title = "已发布", description = "检测当前版本是否已被发布")
    PUBLISHED,

    @Schema(title = "分支版本")
    BRANCH,

    @Schema(title = "正式版本落后", description = "当前页面很旧,但已经有新版本发布")
    RELEASE_OUTDATED
}
