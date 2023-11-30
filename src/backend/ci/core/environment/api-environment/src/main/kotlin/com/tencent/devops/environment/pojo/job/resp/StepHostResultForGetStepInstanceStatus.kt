package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class StepHostResultForGetStepInstanceStatus(
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "IP地址", required = true)
    val ip: String?,
    @ApiModelProperty(value = "IPV6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(
        value = "任务状态", notes = "0-未知错误，1-Agent异常，2-无效主机，3-上次已成功，5-等待执行，7-正在执行，9-执行成功，" +
        "11-执行失败，12-任务下发失败，13-任务超时，15-任务日志错误，16-GSE脚本日志超时，17-GSE文件日志超时，101-脚本执行失败，" +
        "102-脚本执行超时，103-脚本执行被终止，104-脚本返回码非零，202-文件传输失败，203-源文件不存在，301-文件任务系统错误-未分类的，" +
        "303-文件任务超时，310-Agent异常，311-用户名不存在，312-用户密码错误，320-文件获取失败，321-文件超出限制，329-文件传输错误，" +
        "399-任务执行出错，403-任务强制终止成功，404-任务强制终止失败，500-未知状态", required = true
    )
    val status: Int,
    @ApiModelProperty(value = "任务状态描述", required = true)
    val statusDesc: String,
    @ApiModelProperty(
        value = "用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数", allowEmptyValue = true
    )
    val tag: String? = null,
    @ApiModelProperty(value = "基于status与tag字段的分组键，仅用于调用方验证分组内数据数量是否正确，请勿强依赖该字段")
    val groupKey: String?,
    @ApiModelProperty(value = "脚本任务exit code", required = true)
    val exitCode: Int?,
    @ApiModelProperty(value = "开始执行时间", notes = "Unix时间戳，单位毫秒", required = true)
    val startTime: Long,
    @ApiModelProperty(value = "执行结束时间", notes = "Unix时间戳，单位毫秒", required = true)
    val endTime: Long,
    @ApiModelProperty(value = "总耗时", notes = "单位毫秒", required = true)
    val totalTime: Int
)