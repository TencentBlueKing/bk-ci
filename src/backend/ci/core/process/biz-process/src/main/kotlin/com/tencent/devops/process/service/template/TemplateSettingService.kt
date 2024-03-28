package com.tencent.devops.process.service.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 将 template 中 setting 的改动抽出，因为 PAC改动会上新的版本逻辑，可能会涉及
 */
@Service
class TemplateSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val templateCommonService: TemplateCommonService,
    private val templateDao: TemplateDao
) {
    fun updateTemplateSetting(
        projectId: String,
        userId: String,
        templateId: String,
        setting: PipelineSetting
    ): Boolean {
        logger.info("Start to update the template setting - [$projectId|$userId|$templateId]")
        templateCommonService.checkPermission(projectId, userId)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            templateCommonService.checkTemplateName(
                dslContext = context,
                name = setting.pipelineName,
                projectId = projectId,
                templateId = templateId
            )
            templateDao.updateNameAndDescById(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                name = setting.pipelineName,
                desc = setting.desc
            )
            saveTemplatePipelineSetting(userId, setting, true)
        }
        return true
    }

    fun saveTemplatePipelineSetting(
        userId: String,
        setting: PipelineSetting,
        isTemplate: Boolean = false
    ): Int {
        pipelineGroupService.updatePipelineLabel(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            labelIds = setting.labels
        )
        pipelineInfoDao.update(
            dslContext = dslContext,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            userId = userId,
            pipelineName = setting.pipelineName,
            pipelineDesc = setting.desc
        )
        logger.info("Save the template pipeline setting - ($setting)")
        return pipelineSettingDao.saveSetting(dslContext, setting, isTemplate)
    }

    fun insertTemplateSetting(
        context: DSLContext,
        projectId: String,
        templateId: String,
        pipelineName: String,
        isTemplate: Boolean
    ) {
        pipelineSettingDao.insertNewSetting(
            dslContext = context,
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = pipelineName,
            isTemplate = isTemplate,
            failNotifyTypes = pipelineInfoExtService.failNotifyChannel(),
            pipelineAsCodeSettings = try {
                client.get(ServiceProjectResource::class).get(projectId).data
                    ?.properties?.pipelineAsCodeSettings
            } catch (ignore: Throwable) {
                logger.warn("[$projectId]|Failed to sync project|templateId=$templateId", ignore)
                null
            },
            settingVersion = 1
        )
    }

    fun copySetting(setting: PipelineSetting, pipelineId: String, templateName: String): PipelineSetting {
        return setting.copy(pipelineId = pipelineId, pipelineName = templateName)
    }

    fun getTemplateSetting(projectId: String, userId: String, templateId: String): PipelineSetting {
        val setting = pipelineRepositoryService.getSetting(projectId, templateId)
        if (setting == null) {
            logger.warn("Fail to get the template setting - [$projectId|$userId|$templateId]")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
            )
        }
        val groups = pipelineGroupService.getGroups(userId, projectId, templateId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        setting.labels = labels
        return setting
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateSettingService::class.java)
    }
}
