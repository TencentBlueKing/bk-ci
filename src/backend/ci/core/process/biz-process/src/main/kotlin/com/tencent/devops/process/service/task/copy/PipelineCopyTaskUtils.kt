package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary

object PipelineCopyTaskUtils {
    fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType.name}_$resourceId"
    }

    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is ErrorCodeException -> I18nUtil.getCodeLanMessage(
                messageCode = exception.errorCode,
                params = exception.params
            )

            is RemoteServiceException -> exception.errorMessage
            else -> exception.message
        }?.takeIf { it.isNotBlank() } ?: exception.javaClass.simpleName
    }

    fun parseParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam? {
        return task.taskParam?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java)
        }
    }

    fun needAnalyzeAgain(
        task: PipelineBatchTask,
        request: PipelineCopyTaskConfigRequest
    ): Boolean {
        val oldParam = parseParam(task) ?: return false
        return task.status == PipelineBatchTaskStatus.PIPELINE_RESOURCE_ANALYZE_FAILED ||
            oldParam.targetProjectId != request.targetProjectId ||
            oldParam.pipelineCopyStrategy != request.pipelineCopyStrategy
    }

    fun buildSummary(resources: List<PipelineCopyTaskResource>): PipelineCopyTaskSummary {
        return PipelineCopyTaskSummary(
            unprocessedCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.UNPROCESSED
            },
            highRiskCount = resources.count { it.highRisk },
            needCompletionCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_COMPLETION
            },
            needTransferCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_TRANSFER
            },
            autoFinishCount = resources.count {
                it.copyAction == PipelineCopyAction.AUTO_FINISH
            }
        )
    }

    fun buildSummary(
        resources: List<PipelineCopyTaskResource>,
        updates: List<PipelineCopyTaskResourceUpdate>
    ): PipelineCopyTaskSummary {
        if (updates.isEmpty()) {
            return buildSummary(resources)
        }
        val updateMap = updates.associateBy {
            resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }
        return buildSummary(
            resources.map { resource ->
                val update = updateMap[resourceKey(resource.resourceType, resource.resourceId)]
                    ?: return@map resource
                resource.copy(
                    status = update.status ?: resource.status,
                    highRisk = update.highRisk ?: resource.highRisk,
                    copyAction = update.copyAction ?: resource.copyAction
                )
            }
        )
    }

    fun parseSummary(task: PipelineBatchTask): PipelineCopyTaskSummary {
        return task.taskSummary?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyTaskSummary::class.java)
        } ?: PipelineCopyTaskSummary()
    }

    fun buildReplaceResourceMap(
        resources: List<PipelineCopyTaskResource>
    ): Map<String, PipelineDependentResource> {
        return resources.mapNotNull { resource ->
            val targetProjectId = resource.targetProjectId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val targetResourceId = resource.targetResourceId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val targetResourceName = resource.targetResourceName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val key = resourceKey(resource.resourceType, resource.resourceId)
            key to PipelineDependentResource(
                projectId = targetProjectId,
                resourceType = resource.resourceType,
                resourceId = targetResourceId,
                resourceName = targetResourceName
            )
        }.toMap()
    }
}
