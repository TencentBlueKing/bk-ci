package com.tencent.devops.lambda.pojo

data class BuildResultWithPage(
    val total: Long,
    val result: List<BuildResult>
)