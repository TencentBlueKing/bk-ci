package com.tencent.devops.project.pojo

import com.tencent.devops.project.pojo.enums.ProjectReleaseBatchStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目路由发布批次创建记录")
data class ProjectReleaseBatchCreateDTO(
    @get:Schema(title = "发布版本")
    val version: String,
    @get:Schema(title = "项目渠道")
    val channel: String,
    @get:Schema(title = "项目 ID")
    val projectId: String,
    @get:Schema(title = "批次百分比")
    val batchPercent: Int,
    @get:Schema(title = "源集群路由 tag")
    val sourceTag: String,
    @get:Schema(title = "目标集群路由 tag")
    val targetTag: String,
    @get:Schema(title = "发布批次状态")
    val status: ProjectReleaseBatchStatus
)
