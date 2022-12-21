package com.tencent.devops.turbo.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自定义计划任务参数模型")
data class CustomScheduleJobModel(
    @ApiModelProperty("job名称，唯一")
    var jobName: String,

    @ApiModelProperty("触发器名称")
    var triggerName: String,

    @ApiModelProperty("cron表达式")
    var cronExpression: String,

    @ApiModelProperty("计划任务执行类名")
    var jobClassName: String,

    @ApiModelProperty("计划任务执行参数")
    var jobDataMap: Map<String, Any>?
)
