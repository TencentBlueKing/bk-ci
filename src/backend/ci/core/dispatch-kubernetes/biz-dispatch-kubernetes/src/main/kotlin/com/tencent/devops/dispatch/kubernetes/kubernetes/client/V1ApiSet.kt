package com.tencent.devops.dispatch.kubernetes.kubernetes.client

import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api

data class V1ApiSet(
    val coreV1Api: CoreV1Api,
    val batchV1Api: BatchV1Api,
    val appsV1Api: AppsV1Api
)
