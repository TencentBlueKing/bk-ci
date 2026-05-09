package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创建项目路由发布批次请求")
data class ProjectReleaseBatchCreateRequest(
    @get:Schema(title = "发布版本")
    val version: String,
    @get:Schema(title = "项目渠道")
    val channelCode: String,
    @get:Schema(title = "源集群路由 tag")
    val sourceTag: String,
    @get:Schema(title = "目标集群路由 tag")
    val targetTag: String,
    @get:Schema(title = "批次百分比列表")
    val batchPercentages: List<Int>
)
