package com.tencent.devops.process.pojo.pipeline.enums

import com.tencent.devops.common.web.utils.I18nUtil
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细错误类型")
enum class PipelineBatchTaskDetailErrorType {
    @Schema(description = "依赖创建错误")
    DEPENDENCY_CREATE_FAILED,

    @Schema(description = "流水线创建错误")
    PIPELINE_CREATE_FAILED,

    @Schema(description = "系统错误")
    SYSTEM_ERROR;

    fun getI18nName(): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = "pipelineBatchTaskDetailErrorType.$name"
        )
    }
}
