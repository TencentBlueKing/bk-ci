package com.tencent.devops.worker.common.utils

object TenantWorkerUtils {
    /**
     * 是否开启多租户模式
     */
    fun isMultiTenantMode(projectId: String): Boolean = projectId.contains(".")

    public const val DEFAULT_TENANT_ID_FOR_MULTI = "system"
}