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
    val batchPercentages: List<Int>,
    @get:Schema(title = "项目黑名单，名单内项目不参与本次发布批次；不传则不额外排除")
    val blacklist: List<String>? = null,
    @get:Schema(title = "是否仅包含已开启项目；不传则不按 enabled 过滤")
    val enabled: Boolean? = null
)
