package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "OBS运营产品结果")
data class ObsOperationalProductResult(
    @Schema(name = "运营产品")
    val data: List<OperationalProductVO>
)
