package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组交接详细返回体")
data class HandoverGroupDetailVo(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "组ID")
    val iamGroupId: Int,
    @get:Schema(title = "组名称")
    val groupName: String,
    @get:Schema(title = "组描述")
    val groupDesc: String? = null,
    @get:Schema(title = "关联的资源ID")
    val resourceCode: String,
    @get:Schema(title = "关联的资源名称")
    val resourceName: String
)
