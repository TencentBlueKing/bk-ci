package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("红线实体类")
data class QualityRule(
    @ApiModelProperty("hashId", required = true)
    val hashId: String,
    @ApiModelProperty("红线名字", required = true)
    val name: String,
    @ApiModelProperty("红线描述", required = true)
    val desc: String,
    @ApiModelProperty("红线指标列表", required = true)
    val indicators: List<QualityIndicator>,
    @ApiModelProperty("控制点", required = true)
    val controlPoint: RuleControlPoint,
    @ApiModelProperty("流水线范围", required = true)
    val range: List<String>,
    @ApiModelProperty("模板范围", required = true)
    val templateRange: List<String>,
    @ApiModelProperty("操作类型", required = true)
    val operation: RuleOperation,
    @ApiModelProperty("通知类型", required = false)
    val notifyTypeList: List<NotifyType>?,
    @ApiModelProperty("通知组名单", required = false)
    val notifyGroupList: List<String>?,
    @ApiModelProperty("通知人员名单", required = false)
    val notifyUserList: List<String>?,
    @ApiModelProperty("审核通知人员", required = false)
    val auditUserList: List<String>?,
    @ApiModelProperty("审核超时时间", required = false)
    val auditTimeoutMinutes: Int?,
    @ApiModelProperty("红线匹配的id", required = false)
    val gatewayId: String?
) {
    data class RuleControlPoint(
        val hashId: String,
        val name: String,
        val cnName: String,
        val position: ControlPointPosition,
        val availablePosition: List<ControlPointPosition>
    )
}