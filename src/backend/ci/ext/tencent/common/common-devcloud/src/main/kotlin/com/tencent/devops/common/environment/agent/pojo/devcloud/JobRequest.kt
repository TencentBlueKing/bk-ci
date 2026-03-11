package com.tencent.devops.common.environment.agent.pojo.devcloud

data class JobRequest(
    val alias: String? = null,
    val regionId: String? = null,
    val clusterType: String? = null,
    val activeDeadlineSeconds: Int? = null,
    val image: String? = null,
    val registry: Registry? = null,
    val cpu: Int? = null,
    val memory: String? = null,
    val params: JobParam? = null,
    val podNameSelector: String? = null,
    val mountPath: String? = null
)
