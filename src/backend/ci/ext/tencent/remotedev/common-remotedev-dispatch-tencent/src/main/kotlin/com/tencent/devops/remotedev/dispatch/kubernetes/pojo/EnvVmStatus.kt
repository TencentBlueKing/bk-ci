package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum

/**
 * BCS v2 批量查询虚拟机状态返回项 (types.EnvVmStatus)
 *
 * status 与 [EnvStatusEnum] 取值对齐。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvVmStatus(
    val envId: String? = null,
    val status: EnvStatusEnum? = null,
    val ip: String? = null,
    val name: String? = null,
    val clusterId: String? = null,
    val namespace: String? = null
)
