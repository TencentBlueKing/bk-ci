package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudStepHostResultForGetStepInstanceStatus(
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @get:Schema(title = "IP地址", required = true)
    val ip: String?,
    @get:Schema(title = "IPV6地址", required = true)
    val ipv6: String?,
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @get:Schema(title = "Agent ID")
    @JsonProperty("bk_agent_id")
    val bkAgentId: String?,
    @get:Schema(title = "管控区域名称")
    @JsonProperty("bk_cloud_name")
    val bkCloudName: String?,
    @get:Schema(
        title = "任务状态", description = "0-未知错误，1-Agent异常，2-无效主机，3-上次已成功，5-等待执行，7-正在执行，9-执行成功，" +
        "11-执行失败，12-任务下发失败，13-任务超时，15-任务日志错误，16-GSE脚本日志超时，17-GSE文件日志超时，101-脚本执行失败，" +
        "102-脚本执行超时，103-脚本执行被终止，104-脚本返回码非零，202-文件传输失败，203-源文件不存在，301-文件任务系统错误-未分类的，" +
        "303-文件任务超时，310-Agent异常，311-用户名不存在，312-用户密码错误，320-文件获取失败，321-文件超出限制，329-文件传输错误，" +
        "399-任务执行出错，403-任务强制终止成功，404-任务强制终止失败，500-未知状态", required = true
    )
    val status: Int,
    @get:Schema(title = "任务状态描述", required = true)
    @JsonProperty("status_desc")
    val statusDesc: String,
    @get:Schema(title = "用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数")
    val tag: String? = null,
    @get:Schema(title = "脚本任务exit code", required = true)
    @JsonProperty("exit_code")
    val exitCode: Int?,
    @get:Schema(title = "开始执行时间", description = "Unix时间戳，单位毫秒", required = true)
    @JsonProperty("start_time")
    val startTime: Long,
    @get:Schema(title = "执行结束时间", description = "Unix时间戳，单位毫秒", required = true)
    @JsonProperty("end_time")
    val endTime: Long,
    @get:Schema(title = "总耗时", description = "单位毫秒", required = true)
    @JsonProperty("total_time")
    val totalTime: Int
)