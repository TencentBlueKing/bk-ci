package com.tencent.devops.process.service.template

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineSettingVersionService
import com.tencent.devops.process.yaml.utils.NotifyTemplateUtils
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
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val templateCommonService: TemplateCommonService,
    private val templateDao: TemplateDao,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineTemplatePermissionService: PipelineTemplatePermissionService
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
            saveTemplatePipelineSetting(
                context = context,
                userId = userId,
                setting = setting
            )
        }
        return true
    }

    fun saveTemplatePipelineSetting(
        context: DSLContext? = null,
        userId: String,
        setting: PipelineSetting
    ): PipelineSetting {
        val projectId = setting.projectId
        val pipelineId = setting.pipelineId
        // 对齐新旧通知配置，统一根据新list数据保存
        setting.fixSubscriptions()
        modelCheckPlugin.checkSettingIntegrity(setting, projectId)
        val settingVersion = pipelineSettingVersionService.getSettingVersionAfterUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            updateVersion = true,
            setting = setting
        )
        logger.info("Save the template pipeline setting|$pipelineId|settingVersion=$settingVersion|setting=\n$setting")
        val templateName = pipelineRepositoryService.saveSetting(
            context = context,
            userId = userId,
            setting = setting,
            version = settingVersion,
            versionStatus = VersionStatus.RELEASED,
            updateLastModifyUser = true,
            isTemplate = true
        )
        if (templateName.name != templateName.oldName) {
            pipelineTemplatePermissionService.modifyResource(
                userId = userId,
                projectId = projectId,
                templateId = setting.pipelineId,
                templateName = setting.pipelineName
            )
        }
        pipelineGroupService.updatePipelineLabel(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            labelIds = setting.labels
        )
        return setting.copy(version = settingVersion)
    }

    fun insertTemplateSetting(
        context: DSLContext,
        userId: String,
        projectId: String,
        templateId: String,
        pipelineName: String,
        isTemplate: Boolean
    ): PipelineSetting {
        val failNotifyTypes = pipelineInfoExtService.failNotifyChannel()
        val failType = failNotifyTypes.split(",").filter { i -> i.isNotBlank() }
            .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
        val failSubscription = Subscription(
            types = failType,
            groups = emptySet(),
            users = "\${{ci.actor}}",
            content = NotifyTemplateUtils.getCommonShutdownFailureContent()
        )
        val setting = PipelineSetting.defaultSetting(
            projectId = projectId, pipelineId = templateId, pipelineName = pipelineName,
            maxPipelineResNum = null, failSubscription = failSubscription
        )
        return saveTemplatePipelineSetting(
            context = context,
            userId = userId,
            setting = setting
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
