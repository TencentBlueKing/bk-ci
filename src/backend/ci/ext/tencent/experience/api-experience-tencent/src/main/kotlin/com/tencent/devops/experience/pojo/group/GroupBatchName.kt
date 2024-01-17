package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "体验组--批量名称")
data class GroupBatchName(
    @Schema(title = "类型,1--内部人员,3--组织架构")
    val type: Int,
    @Schema(title = "名称列表")
    val names: List<String>
)
