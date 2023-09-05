package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业步骤详细信息")
data class JobStepInfo(
    @ApiModelProperty(value = "作业步骤实例ID", required = true)
    val stepInstanceId: Long,
    @ApiModelProperty(value = "步骤类型", notes = "1：脚本步骤，2：文件步骤，4：SQL步骤", required = true)
    val type: Int,
    @ApiModelProperty(value = "步骤名称", required = true)
    val name: String,
    @ApiModelProperty(
        value = "作业步骤状态码", notes = "1.未执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; " +
        "7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功; 12.步骤强制终止失败", required = true
    )
    val stepStatus: Int,
    @ApiModelProperty(value = "作业步骤实例创建时间", notes = "Unix时间戳，单位毫秒", required = true)
    val creatTime: Long,
    @ApiModelProperty(value = "开始执行时间", notes = "Unix时间戳，单位毫秒", required = true)
    val startTime: Long,
    @ApiModelProperty(value = "执行结束时间", notes = "Unix时间戳，单位毫秒", required = true)
    val endTime: Long,
    @ApiModelProperty(value = "总耗时", notes = "单位毫秒", required = true)
    val totalTime: Int,
    @ApiModelProperty(value = "步骤重试次数", required = true)
    val stepRetries: Int,
    @ApiModelProperty(value = "每个主机的任务执行结果", required = true)
    val stepIpResultList: List<StepIpResult>
)