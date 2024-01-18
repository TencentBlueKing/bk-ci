package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "OBS运营产品结果")
data class ObsOperationalProductResult(
    @get:Schema(title = "运营产品")
    val data: List<OperationalProductVO>
)
