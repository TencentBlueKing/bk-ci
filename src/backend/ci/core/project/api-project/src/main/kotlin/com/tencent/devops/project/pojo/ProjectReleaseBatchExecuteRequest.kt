package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "执行项目路由发布批次请求")
data class ProjectReleaseBatchExecuteRequest(
    @get:Schema(title = "发布版本")
    val version: String,
    @get:Schema(title = "项目渠道")
    val channelCode: String,
    @get:Schema(title = "批次百分比")
    val batchPercent: Int,
    @get:Schema(title = "源集群路由 tag")
    val sourceTag: String,
    @get:Schema(title = "目标集群路由 tag")
    val targetTag: String,
    @get:Schema(title = "是否仅预览，默认预览")
    val dryRun: Boolean = true,
    @get:Schema(title = "是否回滚，默认执行发布")
    val rollback: Boolean = false
)
