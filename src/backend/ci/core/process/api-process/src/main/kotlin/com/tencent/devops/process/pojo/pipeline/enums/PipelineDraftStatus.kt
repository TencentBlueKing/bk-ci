package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线草稿状态")
enum class PipelineDraftStatus {
    @Schema(title = "正常")
    NORMAL,

    @Schema(
        title = "草稿已存在",
        description = "当前操作人和最新草稿的保存人不相同或当前操作人和原草稿的保存人相同,但草稿已超过7天"
    )
    EXISTS,

    @Schema(
        title = "草稿存在冲突",
        description = "同一个草稿版本,但是保存多次,前端还是旧版本"
    )
    CONFLICT,

    @Schema(title = "草稿基线版本落后", description = "草稿基线版本早于当前最新正式版本")
    BASE_OUTDATED,

    @Schema(title = "草稿基线版本落后", description = "草稿基线版本是分支版本")
    BASE_BRANCH,

    @Schema(title = "已发布", description = "检测当前版本是否已被发布")
    PUBLISHED,

    @Schema(title = "分支版本,当前最新版是草稿版本时,编辑时需要提示是否基于分支版本创建草稿")
    BRANCH,

    @Schema(title = "正式版本落后", description = "当前页面很旧,但已经有新版本发布")
    RELEASE_OUTDATED,

    @Schema(title = "版本已删除", description = "当前版本已被删除")
    DELETED
}
