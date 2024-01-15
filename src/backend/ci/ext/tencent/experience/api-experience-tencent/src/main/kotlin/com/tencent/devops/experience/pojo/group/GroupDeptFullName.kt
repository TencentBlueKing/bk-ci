package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "体验组--组织架构")
data class GroupDeptFullName(
    @Schema(description = "名称")
    val name: String,
    @Schema(description = "组织架构")
    val deptFullName: String
)
