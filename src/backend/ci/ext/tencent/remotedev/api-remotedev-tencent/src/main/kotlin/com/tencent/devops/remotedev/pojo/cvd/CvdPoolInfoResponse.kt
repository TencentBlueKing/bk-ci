package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD资源池详情信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdPoolInfoResponse(
    @get:Schema(description = "资源池ID")
    val poolId: String,
    @get:Schema(description = "蓝盾项目ID")
    val bkProjectId: String? = null,
    @get:Schema(description = "网络区域")
    val netArea: String? = null,
    @get:Schema(description = "地域(GZ/TJ/NJ等)")
    val region: String? = null,
    @get:Schema(description = "可用区")
    val zone: String? = null,
    @get:Schema(description = "最大数量")
    val maxCount: Int? = null,
    @get:Schema(description = "资源池状态")
    val poolStatus: String? = null,
    @get:Schema(description = "备注")
    val remark: String? = null,
    @get:Schema(description = "创建时间")
    val createdAt: String? = null,
    @get:Schema(description = "更新时间")
    val updatedAt: String? = null,
    @get:Schema(description = "实例数量")
    val instanceCount: Int? = null,
    @get:Schema(description = "磁盘列表")
    val diskList: List<CvdDiskItem>? = null,
    @get:Schema(description = "实例列表")
    val instanceList: List<CvdInstanceItem>? = null,
    @get:Schema(description = "授权用户列表")
    val userList: List<CvdPoolUser>? = null
)
