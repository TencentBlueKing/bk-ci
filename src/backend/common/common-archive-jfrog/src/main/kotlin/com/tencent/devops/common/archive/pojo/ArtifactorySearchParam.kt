package com.tencent.devops.common.archive.pojo

data class ArtifactorySearchParam(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val regexPath: String,
    val custom: Boolean,
    val executeCount: Int = 1, // 打印日志用到
    val elementId: String = "" // 打印日志用到
)