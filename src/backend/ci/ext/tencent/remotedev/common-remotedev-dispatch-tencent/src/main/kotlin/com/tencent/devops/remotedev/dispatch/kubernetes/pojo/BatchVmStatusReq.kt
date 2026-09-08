package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

/**
 * BCS v2 批量查询虚拟机状态请求体 (POST /api/v2/remotedevenv/vm/status)
 *
 * 只传 uid 列表; uid 非空时 BCS 忽略 zoneId 与分页。
 */
data class BatchVmStatusReq(
    val uid: List<String>
)
