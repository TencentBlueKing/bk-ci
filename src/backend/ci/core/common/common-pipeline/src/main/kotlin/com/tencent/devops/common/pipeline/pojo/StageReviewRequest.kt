package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核-自定义参数")
data class StageReviewRequest(
    @ApiModelProperty("reviewParams", required = true)
    val reviewParams: List<ManualReviewParam>
)
