package com.tencent.devops.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class KubernetesJobStatusResp(
    val deleted: Boolean,
    val status: String,
    @JsonProperty("pod_result")
    val podResult: List<PodResult>?
) {
    data class PodResult(
        val ip: String?,
        val events: List<PodResultEvent>?
    )

    data class PodResultEvent(
        val message: String,
        val reason: String,
        val type: String
    ) {
        override fun toString(): String {
            return "reason: $reason, type: $type"
        }
    }
}

enum class PodStatus(val value: String) {
    // 创建中
    PENDING("pending"),

    // 运行中
    RUNNING("running"),

    // 成功
    SUCCEEDED("succeeded"),

    // 失败
    FAILED("failed");

    companion object {
        fun getStatusFromK8s(status: String?): PodStatus {
            if (status == null || status == "Unknown") {
                return FAILED
            }
            return when (status) {
                "Failed" -> FAILED
                "Succeeded" -> SUCCEEDED
                "Running" -> RUNNING
                "Pending" -> PENDING
                else -> FAILED
            }
        }
    }
}
