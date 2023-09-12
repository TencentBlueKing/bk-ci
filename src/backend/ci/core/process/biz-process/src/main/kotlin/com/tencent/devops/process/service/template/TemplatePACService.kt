package com.tencent.devops.process.service.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.transfer.TransferActionType
import com.tencent.devops.process.pojo.transfer.TransferBody
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.transfer.PipelineTransferYamlService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

/**
 * 因为 PAC 的改动较多，所以将pac中的的代码较大专门抽出来，方便排查错误以及不要影响历史接口
 */
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
        needSetting: Boolean?
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
                setting = null
            )
        }
        val hasPermission = templateCommonService.hasManagerPermission(projectId, userId)
        setting.labels = labels
        setting.hasPermission = hasPermission

        // yaml
        val (_, yaml, _) = transferYamlService.transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = null,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(
                PipelineModelAndSetting(model, setting)
            )
        )

        return TemplatePreviewDetail(
            template = model,
            templateYaml = yaml,
            setting = if (needSetting == true) {
                setting
            } else {
                null
            }
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplatePACService::class.java)
    }
}
