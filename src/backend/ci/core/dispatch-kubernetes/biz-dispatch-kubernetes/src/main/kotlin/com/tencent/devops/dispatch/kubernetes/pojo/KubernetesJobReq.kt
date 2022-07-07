package com.tencent.devops.dispatch.kubernetes.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.dispatch.kubernetes.pojo.base.Registry

data class KubernetesJobReq(
    val alias: String,
    val activeDeadlineSeconds: Int? = null,
    val image: String,
    val registry: Registry? = null,
    val cpu: Int? = null,
    val memory: String? = null,
    val params: KubernetesJobParam? = null,
    val podNameSelector: String? = null
)

data class KubernetesJobParam(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var env: Map<String, String>? = null,
    val command: List<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var nfsVolume: List<NfsVolume>? = null,
    var workDir: String? = "/data/landun/workspace",
    var labels: Map<String, String>? = emptyMap(),
    var ipEnabled: Boolean? = true
) {
    data class NfsVolume(
        val server: String,
        val path: String,
        val mountPath: String
    )
}
