package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目路由发布批次创建结果")
data class ProjectReleaseBatchCreateResult(
    @get:Schema(title = "批次百分比")
    val batchPercent: Int,
    @get:Schema(title = "项目数量")
    val count: Int
)
