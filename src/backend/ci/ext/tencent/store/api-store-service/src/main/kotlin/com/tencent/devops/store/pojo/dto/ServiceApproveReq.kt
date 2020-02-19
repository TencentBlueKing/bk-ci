package com.tencent.devops.store.pojo.dto

import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("审核请求报文")
data class ServiceApproveReq (
    @ApiModelProperty("插件标识")
    val serviceCode: String,
    @ApiModelProperty("审核结果：PASS：通过|REJECT：驳回")
    val result: String,
    @ApiModelProperty("审核结果说明")
    val message: String,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int?,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val serviceType: ServiceTypeEnum,
    @ApiModelProperty("是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件", required = true)
    val defaultFlag: Boolean,
    @ApiModelProperty("服务范围", required = true)
    val serviceScope: List<String>,
    @ApiModelProperty("无构建环境插件是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以", required = false)
    val buildLessRunFlag: Boolean? = null
)