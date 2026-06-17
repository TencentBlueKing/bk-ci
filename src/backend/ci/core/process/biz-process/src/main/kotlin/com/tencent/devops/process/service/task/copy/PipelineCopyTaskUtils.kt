package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDependencyFailed
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDependencyFailedGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDependencyFailedResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskErrorMessage
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskFailedErrorCode
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskFailedMsg
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary

object PipelineCopyTaskUtils {
    fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType.name}_$resourceId"
    }

    fun getErrorMessage(exception: Exception): PipelineBatchTaskErrorMessage {
        return when (exception) {
            is ErrorCodeException -> PipelineBatchTaskFailedErrorCode(
                errorCode = exception.errorCode,
                params = exception.params?.toList()
            )

            else -> PipelineBatchTaskFailedMsg(
                msg = exception.message ?: "unknown error"
            )
        }
    }

    fun buildDependencyFailedMessage(
        failedResources: List<PipelineCopyTaskResource>
    ): PipelineBatchTaskDependencyFailed {
        val details = failedResources.groupBy { it.resourceType }.map { (resourceType, resources) ->
            PipelineBatchTaskDependencyFailedGroup(
                resourceType = resourceType,
                resources = resources.map { resource ->
                    PipelineBatchTaskDependencyFailedResource(
                        resourceId = resource.resourceId,
                        resourceName = resource.resourceName,
                        errorMessage = resource.errorMessage
                    )
                }
            )
        }
        return PipelineBatchTaskDependencyFailed(details = details)
    }

    fun toErrorMessageJson(message: PipelineBatchTaskErrorMessage?): String? {
        return message?.let { JsonUtil.toJson(it, false) }
    }

    fun parseErrorMessage(json: String?): PipelineBatchTaskErrorMessage? {
        if (json.isNullOrBlank()) {
            return null
        }
        return try {
            JsonUtil.to(json, PipelineBatchTaskErrorMessage::class.java)
        } catch (ignored: Exception) {
            PipelineBatchTaskFailedMsg(json)
        }
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

    fun parseSummary(task: PipelineBatchTask): PipelineCopyTaskSummary {
        return task.taskSummary?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineCopyTaskSummary::class.java)
        } ?: PipelineCopyTaskSummary()
    }
}
