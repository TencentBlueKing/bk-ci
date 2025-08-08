package com.tencent.devops.auth.pojo.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "自定义组创建请求体")
data class CustomGroupCreateReq(
    @get:Schema(title = "组名称")
    val groupName: String,
    @get:Schema(title = "组描述")
    val groupDesc: String,
    @get:Schema(title = "操作集合")
    val actions: List<String>
)
