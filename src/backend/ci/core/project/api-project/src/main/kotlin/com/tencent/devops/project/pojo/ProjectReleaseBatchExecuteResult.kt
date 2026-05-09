package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目路由发布批次执行结果")
data class ProjectReleaseBatchExecuteResult(
    @get:Schema(title = "发布版本")
    val version: String,
    @get:Schema(title = "项目渠道")
    val channelCode: String,
    @get:Schema(title = "批次百分比")
    val batchPercent: Int,
    @get:Schema(title = "项目总数")
    val totalProjectCount: Int,
    @get:Schema(title = "本次更新项目数")
    val switchedCount: Int,
    @get:Schema(title = "已在目标 tag 项目数")
    val alreadyDoneCount: Int,
    @get:Schema(title = "当前 tag 不符合预期项目数")
    val skippedCount: Int
)
