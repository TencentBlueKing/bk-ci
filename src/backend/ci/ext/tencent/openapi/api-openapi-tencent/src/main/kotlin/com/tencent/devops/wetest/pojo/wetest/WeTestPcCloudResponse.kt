package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTestPcCloudResponse")
data class WeTestPcCloudResponse(
    @ApiModelProperty("配置详情")
    val records: List<WeTestPcCloud>
)
