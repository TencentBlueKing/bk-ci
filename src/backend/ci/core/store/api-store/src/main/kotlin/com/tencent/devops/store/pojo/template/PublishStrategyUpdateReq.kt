package com.tencent.devops.store.pojo.template

import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板发布策略更新")
data class PublishStrategyUpdateReq(
    @get:Schema(title = "策略", required = true)
    val publishStrategy: UpgradeStrategyEnum
)
