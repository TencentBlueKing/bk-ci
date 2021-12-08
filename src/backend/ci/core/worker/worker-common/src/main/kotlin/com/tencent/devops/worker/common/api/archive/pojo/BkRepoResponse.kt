package com.tencent.devops.worker.common.api.archive.pojo

data class BkRepoResponse<out T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
    val traceId: String? = null
) {
    fun isOk(): Boolean {
        return code == 0
    }

    fun isNotOk(): Boolean {
        return code != 0
    }
}
