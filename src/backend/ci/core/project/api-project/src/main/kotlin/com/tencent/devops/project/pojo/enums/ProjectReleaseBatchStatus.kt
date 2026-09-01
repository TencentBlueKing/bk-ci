package com.tencent.devops.project.pojo.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目路由发布批次状态")
enum class ProjectReleaseBatchStatus {
    @Schema(title = "初始化")
    INIT,

    @Schema(title = "发布中")
    PUBLISHING,

    @Schema(title = "已发布")
    PUBLISHED,

    @Schema(title = "已回滚")
    ROLLBACK
}
