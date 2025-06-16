package com.tencent.devops.worker.common.utils

object TenantWorkerUtils {
    /**
     * 是否开启多租户模式
     */
    fun isMultiTenantMode(projectId: String): Boolean = projectId.contains(".")

    /**
     * 获取租户id
     */
    fun getTenantId(projectId: String): String? = if (isMultiTenantMode(projectId)) {
        projectId.split(".")[0]
    } else {
        null
    }
}