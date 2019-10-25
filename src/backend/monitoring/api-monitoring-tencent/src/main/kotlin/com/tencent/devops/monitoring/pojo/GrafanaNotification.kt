package com.tencent.devops.monitoring.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("grafana监控webhook回调通知消息")
data class GrafanaNotification(
    @ApiModelProperty("标题", required = true)
    val title: String,
    @ApiModelProperty("规则Id", required = true)
    val ruleId: Int,
    @ApiModelProperty("规则名称", required = true)
    var ruleName: String,
    @ApiModelProperty("规则url", required = true)
    var ruleUrl: String,
    @ApiModelProperty("状态", required = true)
    var state: String,
    @ApiModelProperty("图片url", required = false)
    var imageUrl: String?,
    @ApiModelProperty("告警消息", required = true)
    var message: String,
    @ApiModelProperty("grafana监控规则匹配信息", required = false)
    val evalMatches: List<GrafanaEvalMatche>?
)