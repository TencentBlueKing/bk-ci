package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "主机任务执行结果")
data class StepHostResult(
    @get:Schema(title = "IP地址", required = true)
    val ip: String,
    @get:Schema(title = "主机ID")
    val bkHostId: Long?,
    @get:Schema(title = "云区域ID")
    val bkCloudId: Long?,
    @get:Schema(
        title = "作业执行状态", description = "1.Agent异常; 5.等待执行; 7.正在执行; 9.执行成功; 11.执行失败; " +
        "12.任务下发失败; 403.任务强制终止成功; 404.任务强制终止失败", required = true
    )
    val status: Int,
    @get:Schema(title = "用户通过job_success/job_fail函数模板自定义输出的结果", description = "仅脚本任务存在该参数")
    val tag: String? = null,
    @get:Schema(title = "脚本任务exit code", required = true)
    val exitCode: Int,
    @get:Schema(
        title = "主机任务状态码", description = "1.Agent异常; 3.上次已成功; 5.等待执行; 7.正在执行; 9.执行成功; " +
        "11.任务失败; 12.任务下发失败; 13.任务超时; 15.任务日志错误; 101.脚本执行失败; 102.脚本执行超时; 103.脚本执行被终止; " +
        "104.脚本返回码非零; 202.文件传输失败; 203.源文件不存在; 310.Agent异常; 311.用户名不存在; 320.文件获取失败; " +
        "321.文件超出限制; 329.文件传输错误; 399.任务执行出错", required = true
    )
    val errorCode: Int,
    @get:Schema(title = "开始执行时间", description = "Unix时间戳，单位毫秒", required = true)
    val startTime: Long,
    @get:Schema(title = "执行结束时间", description = "Unix时间戳，单位毫秒", required = true)
    val endTime: Long,
    @get:Schema(title = "总耗时", description = "单位毫秒", required = true)
    val totalTime: Int
)