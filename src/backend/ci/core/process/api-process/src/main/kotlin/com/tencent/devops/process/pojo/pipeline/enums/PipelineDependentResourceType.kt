package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线依赖资源类型")
enum class PipelineDependentResourceType {
    @Schema(title = "代码库")
    REPOSITORY,
    @Schema(title = "环境")
    ENVIRONMENT,
    @Schema(title = "构建节点")
    BUILD_NODE,
    @Schema(title = "部署节点")
    DEPLOY_NODE,
    @Schema(title = "凭证")
    CREDENTIAL,
    @Schema(title = "流水线")
    PIPELINE,
    @Schema(title = "流水线组")
    PIPELINE_GROUP,
    @Schema(title = "流水线标签")
    PIPELINE_LABEL,
    @Schema(title = "流水线模板")
    PIPELINE_TEMPLATE
}
