package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "添加组DTO")
data class GroupAddDTO(
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "组描述")
    val groupDesc: String
)
