package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "阶段进度")
data class BuildStageProgressInfo(
    @get:Schema(title = "阶段进度", required = true)
    var stageProgressRete: Double? = null,
    @get:Schema(title = "task进度", required = true)
    var taskProgressList: List<BuildTaskProgressInfo>? = null
)
