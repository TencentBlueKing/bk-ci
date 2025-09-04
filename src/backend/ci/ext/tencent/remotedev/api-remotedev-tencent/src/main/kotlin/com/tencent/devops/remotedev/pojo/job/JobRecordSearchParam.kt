package com.tencent.devops.remotedev.pojo.job

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "任务记录的搜索参数")
data class JobRecordSearchParam(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "页码")
    val page: Int,
    @get:Schema(title = "每页数量")
    val pageSize: Int,
    @get:Schema(title = "任务创建人")
    val creator: String?,
    @get:Schema(title = "任务状态")
    val status: JobRecordStatus?,
    @get:Schema(title = "任务名称")
    val name: String?,
    @get:Schema(title = "任务ID")
    val id: Long?
)

@Schema(title = "任务记录")
data class JobRecord(
    val id: Long,
    val jobName: String,
    val creator: String,
    val status: JobRecordStatus,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?
)
