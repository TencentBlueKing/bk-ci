package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamPair
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核-自定义参数键值对")
data class StageReviewRequest(
    @ApiModelProperty("reviewParams", required = true)
    val reviewParams: List<ManualReviewParamPair>
)
