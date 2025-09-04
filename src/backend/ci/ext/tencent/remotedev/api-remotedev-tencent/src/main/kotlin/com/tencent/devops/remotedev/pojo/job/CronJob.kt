package com.tencent.devops.remotedev.pojo.job

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "周期任务信息")
data class CronJob(
    @get:Schema(title = "ID")
    val id: Long,
    @get:Schema(title = "任务名称")
    val jobName: String,
    @get:Schema(title = "定时表达式")
    val cronExp: String,
    @get:Schema(title = "上次运行时间")
    val lastRunTime: LocalDateTime?,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "更新人")
    val updater: String,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime,
    @get:Schema(title = "执行次数")
    val runTimes: Long
)

@Schema(title = "周期任务搜索参数")
data class CronJobSearchParam(
    val projectId: String,
    val page: Int,
    val pageSize: Int
)
