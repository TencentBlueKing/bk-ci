package com.tencent.bkrepo.helm.pojo.chart

interface ChartOperationRequest {
    val projectId: String
    val repoName: String
    val operator: String
}
