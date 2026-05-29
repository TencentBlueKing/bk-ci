package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务配置请求")
data class PipelineCopyTaskConfigRequest(
    @get:Schema(description = "目标项目ID", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, minLength = 1)
    val targetProjectId: String,
    @get:Schema(description = "任务名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, minLength = 1, maxLength = 100)
    val taskName: String,
    @get:Schema(description = "流水线ID处理策略", required = true)
    val pipelineCopyStrategy: PipelineCopyStrategy
)
