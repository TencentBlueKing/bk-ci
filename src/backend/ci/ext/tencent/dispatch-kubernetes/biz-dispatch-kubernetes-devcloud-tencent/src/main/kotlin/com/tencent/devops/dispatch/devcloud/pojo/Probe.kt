package com.tencent.devops.dispatch.devcloud.pojo

data class Probe(
    val handler: ProbeHandler,
    val initialDelaySeconds: Int = 0,
    val timeoutSeconds: Int = 1,
    val periodSeconds: Int = 2,
    val successThreshold: Int = 1,
    val failureThreshold: Int = 5,
    val terminationGracePeriodSeconds: Long = 10
)
