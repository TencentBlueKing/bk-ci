package com.tencent.devops.plugin.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest测试实例")
data class WetestTaskInstReport(
    @ApiModelProperty("test id")
    val testId: String,
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("构建号")
    val buildNo: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("包版本")
    val version: String,
    @ApiModelProperty("通过率")
    val passingRate: String,
    @ApiModelProperty("对应的任务设置id")
    val taskId: String,
    @ApiModelProperty("测试类型")
    val testType: String,
    @ApiModelProperty("脚本类型")
    val scriptType: String,
    @ApiModelProperty("是否是同步: 0-异步 1-同步")
    val synchronized: String,
    @ApiModelProperty("凭证id")
    val ticketId: String?,
    @ApiModelProperty("启动用户")
    val startUserId: String,
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("开始时间")
    val beginTime: Long,
    @ApiModelProperty("结束时间")
    val endTime: Long? = null
)