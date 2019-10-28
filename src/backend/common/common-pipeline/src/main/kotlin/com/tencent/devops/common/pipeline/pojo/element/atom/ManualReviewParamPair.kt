package com.tencent.devops.common.pipeline.pojo.element.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核-自定义参数键值对")
data class ManualReviewParamPair(
    @ApiModelProperty("key", required = true)
    var key: String? = "",
    @ApiModelProperty("value", required = true)
    var value: String? = ""
)