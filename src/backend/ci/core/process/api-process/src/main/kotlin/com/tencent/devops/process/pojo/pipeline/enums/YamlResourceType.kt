package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "yaml资源类型")
enum class YamlResourceType {
    @Schema(description = "流水线")
    PIPELINE,

    @Schema(description = "流水线模板")
    PIPELINE_TEMPLATE,
    @Schema(description = "stage模板")
    STAGE_TEMPLATE,
    @Schema(description = "job模板")
    JOB_TEMPLATE,
    @Schema(description = "step模板")
    STEP_TEMPLATE;
}
