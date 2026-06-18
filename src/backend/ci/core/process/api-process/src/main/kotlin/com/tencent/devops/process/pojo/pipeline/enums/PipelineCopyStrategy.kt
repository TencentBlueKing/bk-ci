package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源处理策略")
enum class PipelineCopyStrategy(
    val resourceType: PipelineDependentResourceType,
    val copyAction: PipelineCopyAction,
    val highRisk: Boolean
) {
    @Schema(description = "自动创建新流水线ID")
    PIPELINE_CREATE_NEW_ID(
        PipelineDependentResourceType.PIPELINE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用源流水线ID")
    PIPELINE_REUSE_SOURCE_ID(
        PipelineDependentResourceType.PIPELINE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "自动解决冲突")
    PIPELINE_AUTO_RESOLVE_CONFLICT(
        PipelineDependentResourceType.PIPELINE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "跳过,本次不处理")
    PIPELINE_SKIP(
        PipelineDependentResourceType.PIPELINE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名同协议代码库")
    REPOSITORY_REUSE_SAME_NAME_PROTOCOL(
        PipelineDependentResourceType.REPOSITORY,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "创建新代码库")
    REPOSITORY_CREATE_NEW(
        PipelineDependentResourceType.REPOSITORY,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名构建节点")
    BUILD_NODE_REUSE_SAME_NAME(
        PipelineDependentResourceType.BUILD_NODE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "转移构建节点到目标项目")
    BUILD_NODE_MOVE_TO_TARGET_PROJECT(
        PipelineDependentResourceType.BUILD_NODE,
        PipelineCopyAction.NEED_TRANSFER,
        true
    ),

    @Schema(description = "跳过,本次不处理")
    BUILD_NODE_SKIP(
        PipelineDependentResourceType.BUILD_NODE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名构建环境")
    BUILD_ENV_REUSE_SAME_NAME(
        PipelineDependentResourceType.BUILD_ENV,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "新建构建环境不带节点")
    BUILD_ENV_CREATE_WITHOUT_NODE(
        PipelineDependentResourceType.BUILD_ENV,
        PipelineCopyAction.NEED_COMPLETION,
        false
    ),

    @Schema(description = "新建构建环境并转移节点")
    BUILD_ENV_CREATE_AND_MOVE_NODE(
        PipelineDependentResourceType.BUILD_ENV,
        PipelineCopyAction.NEED_TRANSFER,
        true
    ),

    @Schema(description = "新建构建环境并关联同名节点")
    BUILD_ENV_CREATE_AND_REUSE_SAME_NAME_NODE(
        PipelineDependentResourceType.BUILD_ENV,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名部署节点")
    DEPLOY_NODE_REUSE_SAME_NAME(
        PipelineDependentResourceType.DEPLOY_NODE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "转移部署节点到目标项目")
    DEPLOY_NODE_MOVE_TO_TARGET_PROJECT(
        PipelineDependentResourceType.DEPLOY_NODE,
        PipelineCopyAction.NEED_TRANSFER,
        true
    ),

    @Schema(description = "跳过,本次不处理")
    DEPLOY_NODE_SKIP(
        PipelineDependentResourceType.DEPLOY_NODE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名部署环境")
    DEPLOY_ENV_REUSE_SAME_NAME(
        PipelineDependentResourceType.DEPLOY_ENV,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "新建部署环境不带节点")
    DEPLOY_ENV_CREATE_WITHOUT_NODE(
        PipelineDependentResourceType.DEPLOY_ENV,
        PipelineCopyAction.NEED_COMPLETION,
        false
    ),

    @Schema(description = "新建部署环境并转移节点")
    DEPLOY_ENV_CREATE_AND_MOVE_NODE(
        PipelineDependentResourceType.DEPLOY_ENV,
        PipelineCopyAction.NEED_TRANSFER,
        true
    ),

    @Schema(description = "新建部署环境并关联同名节点")
    DEPLOY_ENV_CREATE_AND_REUSE_SAME_NAME_NODE(
        PipelineDependentResourceType.DEPLOY_ENV,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名凭证")
    CREDENTIAL_REUSE_SAME_NAME(
        PipelineDependentResourceType.CREDENTIAL,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "替换为目标项目其他凭证")
    CREDENTIAL_REPLACE_TARGET(
        PipelineDependentResourceType.CREDENTIAL,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "创建新凭证")
    CREDENTIAL_CREATE_NEW(
        PipelineDependentResourceType.CREDENTIAL,
        PipelineCopyAction.AUTO_FINISH,
        true
    ),

    @Schema(description = "自动复用标签，不存在则创建")
    LABEL_AUTO_REUSE_OR_CREATE(
        PipelineDependentResourceType.PIPELINE_LABEL,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "忽略标签")
    LABEL_IGNORE(
        PipelineDependentResourceType.PIPELINE_LABEL,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "自动复用流水线组，不存在则创建")
    PIPELINE_GROUP_AUTO_REUSE_OR_CREATE(
        PipelineDependentResourceType.PIPELINE_GROUP,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "忽略流水线组")
    PIPELINE_GROUP_IGNORE(
        PipelineDependentResourceType.PIPELINE_GROUP,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复用目标项目同名模版")
    PIPELINE_TEMPLATE_REUSE_SAME_NAME(
        PipelineDependentResourceType.PIPELINE_TEMPLATE,
        PipelineCopyAction.AUTO_FINISH,
        false
    ),

    @Schema(description = "复制新模版")
    PIPELINE_TEMPLATE_CREATE_NEW(
        PipelineDependentResourceType.PIPELINE_TEMPLATE,
        PipelineCopyAction.AUTO_FINISH,
        false
    );

    fun support(resourceType: PipelineDependentResourceType): Boolean = this.resourceType == resourceType
}
