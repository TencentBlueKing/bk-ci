package com.tencent.devops.process.trigger.scm.listener

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_CLOSE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_CREATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_CREATE_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_VERSION_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DELETE_VERSION_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_DEPENDENCY_UPGRADE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_RENAME_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_RENAME_SUCCESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_UPDATE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_YAML_PIPELINE_UPDATE_SUCCESS
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.trigger.PipelineTransferFailed
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailCombination
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailMessageCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerValidateDetail
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.yaml.pojo.YamlPipelineActionType
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import org.springframework.stereotype.Service

/**
 * yaml流水线变更事件记录监听器
 */
@Service
class PipelineYamlChangeEventListener(
    private val pipelineTriggerEventService: PipelineTriggerEventService
) : PipelineYamlChangeListener {

    override fun onChangeSuccess(context: PipelineYamlChangeContext) {
        val triggerDetail = with(context) {
            val reasonDetail = getChangeSuccessMsg() ?: return
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.SUCCEED.name,
                pipelineId = pipelineId ?: filePath,
                pipelineName = filePath,
                reason = PipelineTriggerReason.TRIGGER_SUCCESS.name,
                reasonDetail = reasonDetail
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    override fun onChangeError(context: PipelineYamlChangeContext, exception: java.lang.Exception) {
        val exceptionReasonDetail: PipelineTriggerReasonDetail = when {
            exception is PipelineTransferException && !exception.validateDetails.isNullOrEmpty() ->
                PipelineTransferFailed(
                    errorCode = exception.errorCode,
                    // 明细已由 validateDetails 独立承载并按语言渲染，
                    // 此处 params 传空占位符以避免落库时重复保存明细文本
                    params = listOf(""),
                    details = exception.validateDetails!!.map { detail ->
                        PipelineTriggerValidateDetail(
                            messageCode = detail.messageCode,
                            params = detail.params
                        )
                    }
                )
            // 无权限异常的完整信息在 defaultMessage 中，直接存储，避免错误码模板{0}无值渲染
            exception is PermissionForbiddenException ->
                PipelineTriggerFailedMsg(exception.defaultMessage ?: exception.message ?: "unknown error")
            exception is ErrorCodeException -> PipelineTriggerFailedErrorCode(
                errorCode = exception.errorCode,
                params = exception.params?.toList()
            )
            else -> PipelineTriggerFailedMsg(exception.message ?: "unknown error")
        }

        val triggerDetail = with(context) {
            val msgReasonDetail = getChangeFailedMsg() ?: return
            val reasonDetail = PipelineTriggerDetailCombination(
                details = listOf(
                    msgReasonDetail,
                    exceptionReasonDetail
                )
            )
            PipelineTriggerDetail(
                detailId = pipelineTriggerEventService.getDetailId(),
                projectId = projectId,
                eventId = eventId,
                status = PipelineTriggerStatus.FAILED.name,
                pipelineId = pipelineId ?: filePath,
                pipelineName = filePath,
                reason = PipelineTriggerReason.TRIGGER_FAILED.name,
                reasonDetail = reasonDetail
            )
        }
        pipelineTriggerEventService.saveTriggerDetail(triggerDetail)
    }

    /**
     * 获取yaml流水线变更成功说明
     */
    private fun PipelineYamlChangeContext.getChangeSuccessMsg(): PipelineTriggerReasonDetail? {
        val fileType = YamlFileType.getFileType(filePath)
        val linkUrl = getPipelineUrl(fileType = fileType, projectId = projectId, pipelineId = pipelineId)
        return when (actionType) {
            YamlPipelineActionType.CREATE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_CREATE_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.UPDATE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_UPDATE_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.RENAME -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_RENAME_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "", oldFilePath ?: "", filePath)
                )
            }

            YamlPipelineActionType.DELETE_VERSION -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_VERSION_SUCCESS,
                    params = listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.DELETE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_SUCCESS,
                    params = listOf(pipelineName ?: "", pipelineId ?: "")
                )
            }
            else -> null
        }
    }

    /**
     * 获取yaml流水线变更失败原因
     */
    @Suppress("CyclomaticComplexMethod")
    private fun PipelineYamlChangeContext.getChangeFailedMsg(): PipelineTriggerReasonDetail? {
        val fileType = YamlFileType.getFileType(filePath)
        val linkUrl = getPipelineUrl(fileType = fileType, projectId = projectId, pipelineId = pipelineId)
        return when (actionType) {
            YamlPipelineActionType.CREATE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_CREATE_FAILED
                )
            }

            YamlPipelineActionType.UPDATE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_UPDATE_FAILED,
                    listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }

            YamlPipelineActionType.RENAME -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_RENAME_FAILED,
                    params = listOf(linkUrl, pipelineName ?: pipelineId ?: "", oldFilePath ?: "", filePath)
                )
            }

            YamlPipelineActionType.DEPENDENCY_UPGRADE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DEPENDENCY_UPGRADE_FAILED,
                    listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }

            YamlPipelineActionType.DELETE_VERSION -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_VERSION_FAILED,
                    listOf(linkUrl, pipelineName ?: "", versionName ?: "")
                )
            }

            YamlPipelineActionType.DELETE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_DELETE_FAILED,
                    params = listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }
            YamlPipelineActionType.CLOSE -> {
                PipelineTriggerDetailMessageCode(
                    messageCode = BK_YAML_PIPELINE_CLOSE_FAILED,
                    params = listOf(linkUrl, pipelineName ?: pipelineId ?: "")
                )
            }
            else -> null
        }
    }

    private fun getPipelineUrl(
        fileType: YamlFileType,
        projectId: String?,
        pipelineId: String?,
    ): String {
        return if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank()) {
            return ""
        } else {
            if (fileType == YamlFileType.TEMPLATE) {
                "/console/pipeline/$projectId/template/$pipelineId"
            } else {
                "/console/pipeline/$projectId/$pipelineId"
            }
        }
    }
}
