package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板基础信息")
data class PipelineTemplateStrategyUpdateInfo(
    @get:Schema(title = "升级策略-用于研发商店安装的模板", required = true)
    val upgradeStrategy: UpgradeStrategyEnum,
    @get:Schema(title = "配置同步策略-用于研发商店安装的模板", required = true)
    val settingSyncStrategy: UpgradeStrategyEnum
)
