package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.option.JobControlOption

/**
 * 运行时 dependOn 解析用的 Job 摘要
 */
data class DependOnJob(
    val jobId: String?,
    val containerId: String,
    val jobControlOption: JobControlOption
)
