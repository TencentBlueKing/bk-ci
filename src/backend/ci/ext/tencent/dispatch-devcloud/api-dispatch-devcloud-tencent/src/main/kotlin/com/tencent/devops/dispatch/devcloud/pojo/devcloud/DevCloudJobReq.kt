package com.tencent.devops.dispatch.devcloud.pojo.devcloud

data class DevCloudJobReq(
    val alias: String? = null,
    val activeDeadlineSeconds: Int? = null,
    val image: String? = null,
    val registry: Registry? = null,
    val params: JobParam? = null,
    val podNameSelector: String? = null,
    val mountPath: String? = null
)
