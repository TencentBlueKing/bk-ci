package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展版本列表")
data class ServiceVersionListItem(
    @ApiModelProperty("扩展服务ID")
    val serviceId: String,
    @ApiModelProperty("扩展服务标识")
    val serviceCode: String,
    @ApiModelProperty("名称")
    val serviceName: String,
    @ApiModelProperty("版本号")
    val version: String,
    @ApiModelProperty(
        "扩展服务状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val serviceStatus: String,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("创建时间")
    val createTime: String
)