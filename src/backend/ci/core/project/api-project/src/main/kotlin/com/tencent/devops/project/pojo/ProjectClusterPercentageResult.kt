package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "集群项目百分比统计结果")
data class ProjectClusterPercentageResult(
    @get:Schema(title = "项目总数")
    val totalProjectCount: Int,

    @get:Schema(title = "匹配指定 tag 的项目数")
    val tagCount: Int,

    @get:Schema(title = "匹配指定 tag 的项目百分比")
    val percentage: Double
)
