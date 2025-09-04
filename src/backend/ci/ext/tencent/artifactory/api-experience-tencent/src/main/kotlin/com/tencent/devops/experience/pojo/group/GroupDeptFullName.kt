package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "体验组--组织架构")
data class GroupDeptFullName(
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "组织架构")
    val deptFullName: String
)
