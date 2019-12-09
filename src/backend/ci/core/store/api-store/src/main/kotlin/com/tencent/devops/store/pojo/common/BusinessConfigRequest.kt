package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/12/1
 * @Version 1.0
 */
@ApiModel("业务配置请求报文")
data class BusinessConfigRequest(
    @ApiModelProperty("业务", required = true)
    val business: BusinessEnum,
    @ApiModelProperty("业务特性", required = true)
    val feature: String,
    @ApiModelProperty("业务特性取值", required = true)
    val businessValue: String,
    @ApiModelProperty("配置值", required = true)
    val configValue: String,
    @ApiModelProperty("描述", required = true)
    val description: String?
)