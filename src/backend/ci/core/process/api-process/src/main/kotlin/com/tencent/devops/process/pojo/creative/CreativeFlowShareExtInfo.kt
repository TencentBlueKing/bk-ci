package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享扩展信息")
data class CreativeFlowShareExtInfo(
    @get:Schema(title = "分身名称")
    val talentName: String? = null,
    @get:Schema(title = "分身版本号")
    val talentVersion: String? = null,
    @get:Schema(title = "manifest 中声明的条目展示名")
    val flowName: String? = null,
    @get:Schema(title = "manifest 中声明的条目描述")
    val flowDescription: String? = null,
    @get:Schema(title = "manifest 中声明的分类")
    val category: String? = null,
    @get:Schema(title = "源创作流名称，用于目标命名与回执展示")
    val sourcePipelineName: String? = null
)
