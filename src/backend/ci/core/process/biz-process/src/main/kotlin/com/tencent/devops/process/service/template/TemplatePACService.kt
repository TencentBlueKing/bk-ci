package com.tencent.devops.process.service.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.TransferMapper
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.ws.rs.NotFoundException

/**
 * 因为 PAC 的改动较多，所以将pac中的的代码较大专门抽出来，方便排查错误以及不要影响历史接口
 */
@Suppress("ALL")
@Service
class TemplatePACService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val templateDao: TemplateDao,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val transferYamlService: PipelineTransferYamlService,
    private val templateCommonService: TemplateCommonService
) {

    fun previewTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        highlightType: HighlightType?
    ): TemplatePreviewDetail {
        var template = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        val isConstrainedFlag = template.type == TemplateType.CONSTRAINT.name

        if (isConstrainedFlag) {
            try {
                template = templateDao.getLatestTemplate(dslContext, template.srcTemplateId)
            } catch (ignored: NotFoundException) {
                logger.warn("The src template ${template.srcTemplateId} is not exist")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_SOURCE_TEMPLATE_NOT_EXISTS
                )
            }
        }
        val hasPermission = templateCommonService.hasManagerPermission(projectId, userId)
        // model
        val model: Model = objectMapper.readValue(template.template)
        model.name = template.templateName
        model.desc = template.desc
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        model.labels = labels

        // setting
        val setting = pipelineRepositoryService.getSetting(projectId, templateId)
        if (setting == null) {
            logger.warn("Fail to get the template setting - [$projectId|$userId|$templateId]")
            return TemplatePreviewDetail(
                template = model,
                templateYaml = null,
                setting = null,
                hasPermission = hasPermission,
                highlightMarkList = null
            )
        }

        setting.labels = labels

        // yaml
        val (yamlSupported, yaml, yamlInvalidMsg) = try {
            val yml = transferYamlService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = null,
                actionType = TransferActionType.FULL_MODEL2YAML,
                data = TransferBody(
                    PipelineModelAndSetting(model, setting)
                )
            ).yamlWithVersion?.yamlStr
            Triple(true, yml, null)
        } catch (e: Throwable) {
            // 旧流水线可能无法转换，用空YAML代替
            logger.warn("TRANSFER_YAML|$projectId|$userId|FULL_MODEL2YAML", e)
            val msg = if (e is PipelineTransferException) {
                I18nUtil.getCodeLanMessage(
                    messageCode = e.errorCode,
                    params = e.params,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    defaultMessage = e.defaultMessage
                )
            } else null
            Triple(false, null, msg)
        }

        // highlight mark
        val highlightMarkList = mutableListOf<TransferMark>()
        if (yaml != null && highlightType != null) {
            run outside@{
                try {
                    TransferMapper.getYamlLevelOneIndex(yaml).forEach { (key, value) ->
                        when {
                            highlightType == HighlightType.LABEL && key == "label" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.CONCURRENCY && key == "concurrency" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.NOTIFY && key == "notices" -> {
                                highlightMarkList.add(value)
                                return@outside
                            }

                            highlightType == HighlightType.PIPELINE_MODEL && key in pipelineModelKey -> {
                                highlightMarkList.add(value)
                                // pipelineModel 可能多个
                                return@forEach
                            }
                        }
                    }
                } catch (ignore: Throwable) {
                    logger.warn("TRANSFER_YAML|$projectId|$userId", ignore)
                }
            }
        }

        return TemplatePreviewDetail(
            template = model,
            templateYaml = yaml,
            setting = setting,
            hasPermission = hasPermission,
            highlightMarkList = highlightMarkList,
            yamlSupported = yamlSupported,
            yamlInvalidMsg = yamlInvalidMsg
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplatePACService::class.java)
        private val pipelineModelKey = setOf("stages", "jobs", "steps", "finally")
    }
}
