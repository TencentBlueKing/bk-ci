package com.tencent.devops.dispatch.devcloud.pojo

data class Probe(
    val handler: ProbeHandler,
    val initialDelaySeconds: Int,
    val timeoutSeconds: Int,
    val periodSeconds: Int,
    val successThreshold: Int,
    val failureThreshold: Int,
    val terminationGracePeriodSeconds: Long
)
