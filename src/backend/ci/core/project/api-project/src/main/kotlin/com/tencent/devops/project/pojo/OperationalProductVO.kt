package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("运营产品")
data class OperationalProductVO(
    @ApiModelProperty("产品ID")
    @JsonProperty(value = "ProductId", required = true)
    val productId: String,
    @ApiModelProperty("产品ID")
    @JsonProperty(value = "ProductName", required = true)
    val productName: String
)
