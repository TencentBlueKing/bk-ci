package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "运营产品")
data class OperationalProductVO(
    @get:Schema(title = "运营产品ID")
    @JsonProperty(value = "ProductId", required = false)
    val productId: Int? = null,
    @get:Schema(title = "运营产品ID")
    @JsonProperty(value = "ProductName", required = false)
    val productName: String? = null,
    @get:Schema(title = "运营产品ID")
    @JsonProperty(value = "PlanProductId", required = false)
    val planProductId: Int? = null,
    @get:Schema(title = "规划产品名称")
    @JsonProperty(value = "PlanProductName", required = false)
    val planProductName: String? = null,
    @get:Schema(title = "部门ID")
    @JsonProperty(value = "DeptId", required = false)
    val deptId: String? = null,
    @get:Schema(title = "部门名称")
    @JsonProperty(value = "DeptName", required = false)
    val deptName: String? = null,
    @get:Schema(title = "BgId")
    @JsonProperty(value = "BgId", required = false)
    val bgId: String? = null,
    @get:Schema(title = "Bg名称")
    @JsonProperty(value = "BgName", required = false)
    val bgName: String? = null,
    @get:Schema(title = "财务Id")
    val iCosProductCode: String? = null,
    @get:Schema(title = "财务名称")
    val iCosProductName: String? = null,
    @get:Schema(title = "财务代码是否有效")
    val crosCheck: Boolean? = null
)
