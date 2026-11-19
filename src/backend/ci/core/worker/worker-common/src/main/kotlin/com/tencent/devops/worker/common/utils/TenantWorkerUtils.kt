package com.tencent.devops.worker.common.utils

object TenantWorkerUtils {
    /**
     * 是否开启多租户模式
     */
    fun isMultiTenantMode(projectId: String): Boolean = projectId.contains(".")

    public const val DEFAULT_TENANT_ID_FOR_MULTI = "default"

    /**
     * 与服务端 BK_CI_BKREPO_PROJECT_PREFIX 相同。构建容器由调度注入该变量。
     */
    const val BKREPO_PROJECT_PREFIX_ENV = "BK_CI_BKREPO_PROJECT_PREFIX"

    fun bkrepoPlatformProjectId(baseName: String): String {
        val prefix = System.getenv(BKREPO_PROJECT_PREFIX_ENV)?.trim()?.trim('.').orEmpty()
        return if (prefix.isEmpty() || baseName.contains(".")) {
            baseName
        } else {
            "$prefix.$baseName"
        }
    }
}