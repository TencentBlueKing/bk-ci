package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * BCS v2 批量查询虚拟机状态返回体 (batchGetVmStatus)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BatchVmStatusRsp(
    val result: Boolean? = false,
    val code: Int,
    val message: String,
    val data: BatchVmStatusData?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BatchVmStatusData(
    val count: Int? = null,
    val items: List<EnvVmStatus>? = null
)
