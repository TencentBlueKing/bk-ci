package com.tencent.devops.metrics.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("基本查询条件请求报文")
open class BaseQueryReqVO(
    @ApiModelProperty("流水线ID", required = false)
    open var pipelineIds: List<String>? = null,
    @ApiModelProperty("流水线标签", required = false)
    open val pipelineLabelIds: List<Long>? = null,
    @ApiModelProperty("开始时间", required = false)
    open var startTime: String? = null,
    @ApiModelProperty("结束时间", required = false)
    open var endTime: String? = null
)
