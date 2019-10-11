package com.tencent.devops.quality.api.v2.pojo.request

import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("规则创建请求")
data class RuleCreateRequest(
    @ApiModelProperty("规则名称", required = true)
    val name: String,
    @ApiModelProperty("规则描述", required = true)
    val desc: String,
    @ApiModelProperty("指标类型", required = true)
    val indicatorIds: List<CreateRequestIndicator>,
    @ApiModelProperty("控制点", required = true)
    val controlPoint: String,
    @ApiModelProperty("控制点位置", required = true)
    val controlPointPosition: String,
    @ApiModelProperty("生效的流水线id集合", required = true)
    val range: List<String>,
    @ApiModelProperty("生效的流水线模板id集合", required = true)
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
    data class CreateRequestIndicator(
        val hashId: String,
        val operation: String,
        val threshold: String
    )
}