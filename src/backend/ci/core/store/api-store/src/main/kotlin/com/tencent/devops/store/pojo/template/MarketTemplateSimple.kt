package com.tencent.devops.store.pojo.template

import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店模板基本信息")
data class MarketTemplateSimple(
    @get:Schema(title = "研发商店模板ID")
    val templateId: String = "",
    @get:Schema(title = "研发商店模板状态")
    val status: TemplateStatusEnum
)
