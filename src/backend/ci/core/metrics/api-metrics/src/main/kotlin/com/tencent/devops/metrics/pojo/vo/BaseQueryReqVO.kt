package com.tencent.devops.metrics.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "基本查询条件请求报文")
open class BaseQueryReqVO(
    @Schema(title = "流水线ID", required = false)
    open var pipelineIds: List<String>? = null,
    @Schema(title = "流水线标签", required = false)
    open val pipelineLabelIds: List<Long>? = null,
    @Schema(title = "开始时间", required = false)
    open var startTime: String? = null,
    @Schema(title = "结束时间", required = false)
    open var endTime: String? = null
)
