package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "实例化-推荐版本号")
data class TemplateInstanceRecommendedVersion(
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "主版本")
    var major: Int = 0,
    @get:Schema(title = "特性版本")
    var minor: Int = 0,
    @get:Schema(title = "修正版本")
    var fix: Int = 0,
    @get:Schema(title = "构建号")
    val buildNo: BuildNo
)
