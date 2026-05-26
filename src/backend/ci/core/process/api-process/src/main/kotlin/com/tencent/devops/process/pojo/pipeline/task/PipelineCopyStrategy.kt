package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源处理策略")
enum class PipelineCopyStrategy(
    val resourceType: PipelineDependentResourceType
) {
    @Schema(description = "自动创建新流水线ID")
    PIPELINE_CREATE_NEW_ID(PipelineDependentResourceType.PIPELINE),
    @Schema(description = "复用源流水线ID")
    PIPELINE_REUSE_SOURCE_ID(PipelineDependentResourceType.PIPELINE),

    @Schema(description = "复用目标项目同名同协议代码库")
    REPOSITORY_REUSE_SAME_NAME_PROTOCOL(PipelineDependentResourceType.REPOSITORY),
    @Schema(description = "创建新代码库")
    REPOSITORY_CREATE_NEW(PipelineDependentResourceType.REPOSITORY),

    @Schema(description = "复用目标项目同名构建节点")
    BUILD_NODE_REUSE_SAME_NAME(PipelineDependentResourceType.BUILD_NODE),
    @Schema(description = "转移构建节点到目标项目")
    BUILD_NODE_MOVE_TO_TARGET_PROJECT(PipelineDependentResourceType.BUILD_NODE),

    @Schema(description = "复用目标项目同名环境")
    ENVIRONMENT_REUSE_SAME_NAME(PipelineDependentResourceType.ENVIRONMENT),
    @Schema(description = "新建环境不带节点")
    ENVIRONMENT_CREATE_WITHOUT_NODE(PipelineDependentResourceType.ENVIRONMENT),
    @Schema(description = "新建环境并转移节点")
    ENVIRONMENT_CREATE_AND_MOVE_NODE(PipelineDependentResourceType.ENVIRONMENT),

    @Schema(description = "复用目标项目同名部署节点")
    DEPLOY_NODE_REUSE_SAME_NAME(PipelineDependentResourceType.DEPLOY_NODE),
    @Schema(description = "转移部署节点到目标项目")
    DEPLOY_NODE_MOVE_TO_TARGET_PROJECT(PipelineDependentResourceType.DEPLOY_NODE),

    @Schema(description = "复用目标项目同名凭证")
    CREDENTIAL_REUSE_SAME_NAME(PipelineDependentResourceType.CREDENTIAL),
    @Schema(description = "替换为目标项目其他凭证")
    CREDENTIAL_REPLACE_TARGET(PipelineDependentResourceType.CREDENTIAL),
    @Schema(description = "创建新凭证")
    CREDENTIAL_CREATE_NEW(PipelineDependentResourceType.CREDENTIAL),

    @Schema(description = "自动复用标签，不存在则创建")
    LABEL_AUTO_REUSE_OR_CREATE(PipelineDependentResourceType.PIPELINE_LABEL),
    @Schema(description = "忽略标签")
    LABEL_IGNORE(PipelineDependentResourceType.PIPELINE_LABEL),

    @Schema(description = "自动复用流水线组，不存在则创建")
    PIPELINE_GROUP_AUTO_REUSE_OR_CREATE(PipelineDependentResourceType.PIPELINE_GROUP),
    @Schema(description = "忽略流水线组")
    PIPELINE_GROUP_IGNORE(PipelineDependentResourceType.PIPELINE_GROUP);

    fun support(resourceType: PipelineDependentResourceType): Boolean = this.resourceType == resourceType
}
