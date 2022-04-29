package com.tencent.devops.dispatch.bcs.pojo

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * bcs 创建job参数
 */
data class DispatchJobReq(
    val alias: String,
    val activeDeadlineSeconds: Int? = null,
    val image: String,
    val registry: DockerRegistry,
    val params: JobParam? = null,
    val podNameSelector: String,
    val mountPath: String? = null
)

data class JobParam(
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
        val path: String? = null,
        val mountPath: String? = null
    )
}
