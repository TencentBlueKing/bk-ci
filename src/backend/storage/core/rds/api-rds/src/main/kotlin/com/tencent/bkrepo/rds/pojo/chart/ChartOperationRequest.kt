package com.tencent.bkrepo.rds.pojo.chart

interface ChartOperationRequest {
    val projectId: String
    val repoName: String
    val operator: String
}
