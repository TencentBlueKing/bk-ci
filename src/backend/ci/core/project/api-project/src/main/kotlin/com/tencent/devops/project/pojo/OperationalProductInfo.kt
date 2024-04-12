package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "运营产品实体")
data class OperationalProductInfo(
    @get:Schema(title = "运营产品ID")
    val productId: Int,
    @get:Schema(title = "运营产品名称")
    val productName: String,
    @get:Schema(title = "规划产品名称")
    val planProductName: String,
    @get:Schema(title = "部门名称")
    val deptName: String,
    @get:Schema(title = "BG名称")
    val bgName: String
)
