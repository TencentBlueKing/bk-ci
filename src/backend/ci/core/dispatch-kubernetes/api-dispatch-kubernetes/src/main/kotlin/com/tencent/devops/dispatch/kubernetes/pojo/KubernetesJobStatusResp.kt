package com.tencent.devops.dispatch.kubernetes.pojo

data class KubernetesJobStatusResp(
    val deleted: Boolean,
    val status: String,
    val pod_result: List<PodResult>?
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
