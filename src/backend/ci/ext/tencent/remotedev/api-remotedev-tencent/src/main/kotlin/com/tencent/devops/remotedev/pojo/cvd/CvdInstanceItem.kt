package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD用户实例信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdInstanceItem(
    @get:Schema(description = "磁盘ID")
    val diskId: String? = null,
    @get:Schema(description = "磁盘所属资源池ID")
    val poolId: String? = null,
    @get:Schema(description = "磁盘状态")
    val diskStatus: String? = null,
    @get:Schema(description = "当前绑定实例ID")
    val attachedInstanceId: String? = null,
    @get:Schema(description = "地域(GZ/TJ/NJ等)")
    val region: String? = null,
    @get:Schema(description = "可用区")
    val zone: String? = null
)
