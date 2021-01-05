package com.tencent.devops.common.pipeline.pojo.element.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核-自定义参数-下拉框列表剑")
data class ManualReviewParamPair(
    @ApiModelProperty("参数名", required = true)
    val key: String,
    @ApiModelProperty("参数内容", required = true)
    val value: String
)