package com.tencent.devops.dispatch.kubernetes.kubernetes.model.job

import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.PodData

data class JobData(
    val apiVersion: String,
    val name: String,
    val nameSpace: String,
    val backoffLimit: Int,
    val activeDeadlineSeconds: Int?,
    val pod: PodData
)
