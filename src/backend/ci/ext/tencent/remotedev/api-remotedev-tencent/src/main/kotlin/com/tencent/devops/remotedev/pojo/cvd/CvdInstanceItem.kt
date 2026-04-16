package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD用户实例信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdInstanceItem(
    @get:Schema(description = "实例ID")
    val instanceId: String? = null,
    @get:Schema(description = "所属资源池ID")
    val poolId: String? = null,
    @get:Schema(description = "实例状态")
    val instanceStatus: String? = null,
    @get:Schema(description = "机器类型")
    val machineType: String? = null,
    @get:Schema(description = "当前挂载的磁盘ID")
    val curDiskId: String? = null,
    @get:Schema(description = "当前使用用户")
    val curUser: String? = null,
    @get:Schema(description = "蓝盾项目ID")
    val bkProjectId: String? = null,
    @get:Schema(description = "实例IP")
    val ip: String? = null,
    @get:Schema(description = "云主机实例ID")
    val cvmId: String? = null,
    @get:Schema(description = "地域")
    val region: String? = null,
    @get:Schema(description = "可用区")
    val zone: String? = null,
    @get:Schema(description = "当前会话ID")
    val curSessionId: String? = null,
    @get:Schema(description = "专区ID")
    val tenantId: Int? = null
)
