package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("OBS运营产品结果")
data class ObsOperationalProductResult(
    @ApiModelProperty("运营产品")
    val data: List<OperationalProductVO>
)
