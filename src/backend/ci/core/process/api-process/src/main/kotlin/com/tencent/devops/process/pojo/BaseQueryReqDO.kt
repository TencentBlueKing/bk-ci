package com.tencent.devops.metrics.pojo.`do`

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("基本查询条件请求报文")
open class BaseQueryReqDO(
    @ApiModelProperty("流水线ID", required = false)
    open val pipelineIds: List<String>? = null,
    @ApiModelProperty("流水线标签", required = false)
    open val pipelineLabelIds: List<Long>? = null,
    @ApiModelProperty("开始时间", required = false)
    open var startTime: String? = null,
    @ApiModelProperty("结束时间", required = false)
    open var endTime: String? = null
)