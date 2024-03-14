package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class GetStepInstanceStatusResult(
    @get:Schema(
        title = "作业步骤状态码", description = "1-未执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，" +
        "7-等待用户，8-手动结束，9-状态异常，10-步骤强制终止中，11-步骤强制终止成功，12-步骤强制终止失败"
    )
    val status: Int,
    @get:Schema(title = "总耗时，单位毫秒")
    val totalTime: Int,
    @get:Schema(title = "步骤名称")
    val name: String,
    @get:Schema(title = "作业步骤实例ID")
    val stepInstanceId: Long,
    @get:Schema(title = "步骤重试次数")
    val executeCount: Int,
    @get:Schema(title = "作业步骤实例创建时间，Unix时间戳，单位毫秒")
    val createTime: Long,
    @get:Schema(title = "执行结束时间，Unix时间戳，单位毫秒")
    val endTime: Long,
    @get:Schema(title = "步骤类型", description = "1-脚本步骤；2-文件步骤；4-SQL步骤")
    val type: Int,
    @get:Schema(title = "开始执行时间，Unix时间戳，单位毫秒")
    val startTime: Long,
    @get:Schema(title = "任务执行结果分组列表")
    val stepResultGroupList: List<StepResultGroup>?
)