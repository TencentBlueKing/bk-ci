package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "实例化-推荐版本号")
data class TemplateInstanceRecommendedVersion(
    @get:Schema(title = "是否为入参", required = true)
    var allowModifyAtStartup: Boolean? = null,
    @get:Schema(title = "主版本")
    var major: Int? = null,
    @get:Schema(title = "特性版本")
    var minor: Int? = null,
    @get:Schema(title = "修正版本")
    var fix: Int? = null,
    @get:Schema(title = "构建版本号")
    var buildNo: BuildNo? = null
) {
    data class BuildNo(
        @get:Schema(title = "初始值")
        val buildNo: Int = 0
    )
}
