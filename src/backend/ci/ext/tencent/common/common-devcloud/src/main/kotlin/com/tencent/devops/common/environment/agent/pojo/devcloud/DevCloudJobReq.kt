package com.tencent.devops.dispatcher.devcloud.pojo.devcloud

import com.tencent.bk.devops.plugin.docker.pojo.job.request.JobParam

data class DevCloudJobReq(
    val alias: String? = null,
    val activeDeadlineSeconds: Int? = null,
    val image: String? = null,
    val registry: Registry? = null,
    val params: JobParam? = null,
    val podNameSelector: String? = null,
    val mountPath: String? = null
)
