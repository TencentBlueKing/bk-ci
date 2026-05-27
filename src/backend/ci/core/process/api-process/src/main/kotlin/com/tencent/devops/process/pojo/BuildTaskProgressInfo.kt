package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "task进度")
data class BuildTaskProgressInfo(
    @get:Schema(title = "task进度", required = true)
    var taskProgressRete: Double? = null,
    @get:Schema(title = "task名称", required = true)
    var taskName: String? = null,
    @get:Schema(title = "Job执行顺序", required = true)
    var jobExecutionOrder: String? = null,
    @get:Schema(title = "task进度明细", required = false)
    var progressDetail: BuildTaskProgressDetail? = null
)
