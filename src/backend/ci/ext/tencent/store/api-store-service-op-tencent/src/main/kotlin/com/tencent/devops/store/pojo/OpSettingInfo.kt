package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import io.swagger.annotations.ApiModelProperty

data class OpSettingInfo(
    @ApiModelProperty("扩展类型")
    val type: ServiceTypeEnum?,
    @ApiModelProperty("扩展服务类型：0：官方自研，1：第三方", required = true)
    val serviceTypeEnum: ServiceTypeEnum,
    @ApiModelProperty("是否公共", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐", required = true)
    val recommendFlag: Boolean,
    @ApiModelProperty("是否官方认证", required = true)
    val certificationFlag: Boolean,
    @ApiModelProperty("权重", required = true)
    val weight: Float
)