package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD资源池实例信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdPoolInstance(
    @get:Schema(description = "实例ID")
    val instanceId: String,
    @get:Schema(description = "资源池ID")
    val poolId: String,
    @get:Schema(description = "实例状态")
    val instanceStatus: String? = null,
    @get:Schema(description = "机型")
    val machineType: String? = null,
    @get:Schema(description = "当前磁盘ID")
    val curDiskId: String? = null,
    @get:Schema(description = "当前使用用户")
    val curUser: String? = null
)
