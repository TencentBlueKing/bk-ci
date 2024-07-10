package com.tencent.devops.remotedev.pojo.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

data class JobDetail(
    val jobActionType: JobActionType,
    val data: JobActionDetail?
)

interface JobActionDetail

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "流水线任务详情")
data class JobPipelineDetail(
    @get:Schema(title = "标准运维任务详情")
    var sopData: List<JobPipelineSopDetail>?
) : JobActionDetail

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "流水线任务中标准运维任务详情")
data class JobPipelineSopDetail(
    @get:Schema(title = "job任务详情")
    val jobTasks: List<JobPipelineSopJobTask>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "流水线任务中标准运维任务中Job任务详情")
data class JobPipelineSopJobTask(
    @get:Schema(title = "执行成功的机器")
    val successIps: List<JobPipelineSopJobIpData>?,
    @get:Schema(title = "执行失败的机器")
    val failedIps: List<JobPipelineSopJobIpData>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "流水线任务中标准运维任务中Job任务Ip执行数据")
data class JobPipelineSopJobIpData(
    @get:Schema(title = "机器IP")
    val ip: String,
    @get:Schema(title = "机器具体状态码")
    val status: Int,
    @get:Schema(title = "失败时机器具体错误码")
    val errorCode: Int?,
    @get:Schema(title = "失败时机器具体失败日志")
    val log: String?,
    @get:Schema(title = "开始时间")
    val startTime: Long,
    @get:Schema(title = "结束时间")
    val endTime: Long
)