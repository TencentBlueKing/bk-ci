package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import io.swagger.annotations.ApiModelProperty

data class UserQualityRule(
    val hashId: String,
    val name: String,
    val desc: String,
    val indicators: List<QualityIndicator>,
    val controlPoint: QualityRule.RuleControlPoint,
    @ApiModelProperty("生效的流水线id集合", required = true)
    val range: List<RangeItem>,
    @ApiModelProperty("生效的流水线模板id集合", required = true)
    val templateRange: List<RangeItem>,
    @ApiModelProperty("生效的流水线和模板对应的流水线总数", required = true)
    val pipelineCount: Int,
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
    @ApiModelProperty("最新拦截状态", required = false)
    var interceptRecent: String?,
    @ApiModelProperty("红线匹配的id", required = false)
    val gatewayId: String?
) {
    data class RangeItem(
        val id: String, // 流水线或者模板id
        val name: String
    )
}