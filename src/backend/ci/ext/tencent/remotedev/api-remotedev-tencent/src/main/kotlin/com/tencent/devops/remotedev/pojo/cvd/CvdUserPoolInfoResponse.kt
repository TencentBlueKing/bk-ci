package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD用户资源池信息响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdUserPoolInfoResponse(
    @get:Schema(
        description = "用户在各资源池的状态，key为poolId，" +
            "value为状态枚举(UNCLAIMED/CLAIMING/" +
            "CLAIM_FAILED/CLAIMED/RETURNING/RETURN_FAILED)"
    )
    val status: Map<String, String>? = null,
    @get:Schema(description = "用户所在资源池ID列表")
    val poolIdList: List<String>? = null,
    @get:Schema(
        description = "用户所在资源池的实例数量，key为poolId"
    )
    val cvdCount: Map<String, CvdCountItem>? = null,
    @get:Schema(description = "用户可用磁盘列表")
    val diskList: List<CvdDiskItem>? = null,
    @get:Schema(description = "用户实例列表")
    val instanceList: List<CvdInstanceItem>? = null
)
