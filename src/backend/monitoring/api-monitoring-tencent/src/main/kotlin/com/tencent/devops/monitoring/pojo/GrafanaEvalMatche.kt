package com.tencent.devops.monitoring.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("grafana监控规则匹配信息")
data class GrafanaEvalMatche(
    @ApiModelProperty("监控对象", required = true)
    val metric: String,
    @ApiModelProperty("监控对象数值", required = true)
    val value: String,
    @ApiModelProperty("标签", required = false)
    var tags: Map<String, Any>?
)