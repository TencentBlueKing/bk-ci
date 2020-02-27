package com.tencent.devops.store.pojo.dto

import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("审核请求报文")
data class ServiceApproveReq(
    @ApiModelProperty("插件标识")
    val serviceCode: String,
    @ApiModelProperty("审核结果：PASS：通过|REJECT：驳回")
    val result: String,
    @ApiModelProperty("审核结果说明")
    val message: String,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int?,
    @ApiModelProperty("扩展类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val serviceType: ServiceTypeEnum,
    @ApiModelProperty("是否官方认证，true：官方推荐 false：官方不推荐", required = true)
    val certificationFlag: Boolean,
    @ApiModelProperty("是否公共， TRUE：是 FALSE：否", required = false)
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐， TRUE：可以 FALSE：不可以", required = false)
    val recommendFlag: Boolean? = null
)