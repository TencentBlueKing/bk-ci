package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业实例基本信息")
data class JobCloudJobInstance(
    @ApiModelProperty(value = "作业实例名称", required = true)
    val name: String,
    @ApiModelProperty(
        value = "作业状态码", notes = "1.未执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; " +
        "7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功", required = true
    )
    val status: Int,
    @ApiModelProperty(value = "作业创建时间", notes = "Unix时间戳，单位毫秒", required = true)
    @JsonProperty("create_time")
    val createTime: Long,
    @ApiModelProperty(value = "开始执行时间", notes = "Unix时间戳，单位毫秒", required = true)
    @JsonProperty("start_time")
    val startTime: Long,
    @ApiModelProperty(value = "执行结束时间", notes = "Unix时间戳，单位毫秒", required = true)
    @JsonProperty("end_time")
    val endTime: Long,
    @ApiModelProperty(value = "总耗时", notes = "单位毫秒", required = true)
    @JsonProperty("total_time")
    val totalTime: Int,
    @ApiModelProperty(value = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @ApiModelProperty(value = "业务ID", required = true)
    @JsonProperty("bk_biz_id")
    val bkBizId: Long,
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集", required = true)
    @JsonProperty("bk_scope_type")
    var bkScopeType: String,
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    @JsonProperty("bk_scope_id")
    var bkScopeId: String
)