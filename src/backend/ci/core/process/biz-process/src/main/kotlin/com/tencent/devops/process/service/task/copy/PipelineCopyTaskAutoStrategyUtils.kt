package com.tencent.devops.process.service.task.copy

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskAutoStrategyResult
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSaveResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSaveResourceRequest

/**
 * 流水线复制任务一键设置资源策略工具
 */
object PipelineCopyTaskAutoStrategyUtils {

    fun resolveAutoCopyStrategy(
        resourceType: PipelineDependentResourceType,
        targetNameExists: Boolean
    ): PipelineCopyStrategy? {
        return when (resourceType) {
            PipelineDependentResourceType.PIPELINE,
            PipelineDependentResourceType.PIPELINE_LABEL,
            PipelineDependentResourceType.PIPELINE_GROUP -> null

            PipelineDependentResourceType.BUILD_NODE -> if (targetNameExists) {
                PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME
            } else {
                null
            }

            PipelineDependentResourceType.DEPLOY_NODE -> if (targetNameExists) {
                PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME
            } else {
                null
            }

            PipelineDependentResourceType.REPOSITORY -> if (targetNameExists) {
                PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL
            } else {
                PipelineCopyStrategy.REPOSITORY_CREATE_NEW
            }

            PipelineDependentResourceType.BUILD_ENV -> if (targetNameExists) {
                PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME
            } else {
                PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE
            }

            PipelineDependentResourceType.DEPLOY_ENV -> if (targetNameExists) {
                PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME
            } else {
                PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE
            }

            PipelineDependentResourceType.CREDENTIAL -> if (targetNameExists) {
                PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME
            } else {
                PipelineCopyStrategy.CREDENTIAL_CREATE_NEW
            }

            PipelineDependentResourceType.PIPELINE_TEMPLATE -> if (targetNameExists) {
                PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME
            } else {
                PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW
            }
        }
    }

    fun buildAutoResourceStrategyResult(
        resources: List<PipelineCopyTaskResource>
    ): Pair<PipelineCopyTaskSaveResourceRequest, PipelineCopyTaskAutoStrategyResult> {
        val unprocessedResources = resources.filter { it.copyStrategy == null }
        val saveResources = unprocessedResources.mapNotNull { resource ->
            val copyStrategy = resolveAutoCopyStrategy(
                resourceType = resource.resourceType,
                targetNameExists = resource.targetNameExists
            ) ?: return@mapNotNull null
            PipelineCopyTaskSaveResource(
                resourceType = resource.resourceType,
                resourceId = resource.resourceId,
                resourceName = resource.resourceName,
                copyStrategy = copyStrategy
            )
        }
        val unprocessedLabelCount = unprocessedResources.count {
            it.resourceType == PipelineDependentResourceType.PIPELINE_LABEL
        }
        val unprocessedGroupCount = unprocessedResources.count {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP
        }
        var processedCount = saveResources.size
        val pipelineLabelCopyStrategy = if (unprocessedLabelCount > 0) {
            processedCount++
            PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE
        } else {
            null
        }
        val pipelineGroupCopyStrategy = if (unprocessedGroupCount > 0) {
            processedCount++
            PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE
        } else {
            null
        }
        val request = PipelineCopyTaskSaveResourceRequest(
            pipelineLabelCopyStrategy = pipelineLabelCopyStrategy,
            pipelineGroupCopyStrategy = pipelineGroupCopyStrategy,
            resources = saveResources
        )
        val result = PipelineCopyTaskAutoStrategyResult(
            processedCount = processedCount,
            nodeNotSetCount = unprocessedResources.count {
                (it.resourceType == PipelineDependentResourceType.BUILD_NODE ||
                        it.resourceType == PipelineDependentResourceType.DEPLOY_NODE) &&
                        !it.targetNameExists
            },
            pipelineConflictCount = unprocessedResources.count {
                it.resourceType == PipelineDependentResourceType.PIPELINE &&
                        (it.targetNameExists || it.targetIdExists)
            }
        )
        return Pair(request, result)
    }
}
