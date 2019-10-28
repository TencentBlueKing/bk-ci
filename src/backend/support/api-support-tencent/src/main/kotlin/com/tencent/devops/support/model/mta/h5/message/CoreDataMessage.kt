package com.tencent.devops.support.model.mta.h5.message

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("应用历史趋势查询消息")
data class CoreDataMessage(
    @ApiModelProperty("开始日期(时间戳形式毫秒)")
    var startDate: Long,
    @ApiModelProperty("结束日期(时间戳形式毫秒)")
    var endDate: Long
)