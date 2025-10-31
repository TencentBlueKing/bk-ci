/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.pipeline

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_DEFAULT
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import com.tencent.devops.process.pojo.setting.JobCommonSetting
import com.tencent.devops.process.pojo.setting.PipelineCommonSetting
import com.tencent.devops.process.pojo.setting.StageCommonSetting
import com.tencent.devops.process.pojo.setting.TaskCommonSetting
import com.tencent.devops.process.pojo.setting.TaskComponentCommonSetting
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.strategy.context.UserPipelinePermissionCheckContext
import com.tencent.devops.process.strategy.factory.UserPipelinePermissionCheckStrategyFactory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineSettingFacadeService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineCommonSettingConfig: PipelineCommonSettingConfig,
    private val stageCommonSettingConfig: StageCommonSettingConfig,
    private val jobCommonSettingConfig: JobCommonSettingConfig,
    private val taskCommonSettingConfig: TaskCommonSettingConfig,
    private val auditService: AuditService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    private val logger = LoggerFactory.getLogger(PipelineSettingFacadeService::class.java)

    /**
     * 修改配置时需要返回具体的版本号用于传递
     */
    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceIds = "#setting?.pipelineId",
            instanceNames = "#setting?.pipelineName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#setting?.projectId")],
        scopeId = "#setting?.projectId",
        content = ActionAuditContent.PIPELINE_EDIT_SAVE_SETTING_CONTENT
    )
    fun saveSetting(
        context: DSLContext? = null,
        userId: String,
        projectId: String,
        pipelineId: String,
        setting: PipelineSetting,
        versionStatus: VersionStatus = VersionStatus.RELEASED,
        checkPermission: Boolean = true,
        updateLastModifyUser: Boolean? = true,
        dispatchPipelineUpdateEvent: Boolean = true,
        updateLabels: Boolean = true,
        updateVersion: Boolean = true
    ): PipelineSetting {
        if (checkPermission) {
            val language = I18nUtil.getLanguage(userId)
            val permission = AuthPermission.EDIT
            checkEditPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language,
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(language),
                        pipelineId
                    )
                )
            )
        }
        // 对齐新旧通知配置，统一根据新list数据保存
        setting.fixSubscriptions()
        modelCheckPlugin.checkSettingIntegrity(setting, projectId)
        ActionAuditContext.current().setInstance(setting)
        val settingVersion = pipelineSettingVersionService.getSettingVersionAfterUpdate(
            projectId = projectId,
            pipelineId = pipelineId,
            updateVersion = updateVersion,
            setting = setting
        )
        val pipelineName = pipelineRepositoryService.saveSetting(
            context = context,
            userId = userId,
            setting = setting.copy(version = settingVersion),
            version = settingVersion,
            versionStatus = versionStatus,
            updateLastModifyUser = updateLastModifyUser,
            isTemplate = false
        )

        if (pipelineName.name != pipelineName.oldName) {
            auditService.createAudit(
                Audit(
                    resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                    resourceId = setting.pipelineId,
                    resourceName = pipelineName.name,
                    userId = userId,
                    action = "edit",
                    actionContent = "Rename (${pipelineName.oldName})",
                    projectId = setting.projectId
                )
            )
            if (checkPermission) {
                pipelinePermissionService.modifyResource(
                    projectId = setting.projectId,
                    pipelineId = setting.pipelineId,
                    pipelineName = setting.pipelineName
                )
            }
        }

        if (updateLabels) {
            pipelineGroupService.updatePipelineLabel(
                userId = userId,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                labelIds = setting.labels
            )
        }

        // 刷新流水线组
        pipelineViewGroupService.updateGroupAfterPipelineUpdate(
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            pipelineName = setting.pipelineName,
            creator = userId,
            userId = userId
        )

        if (dispatchPipelineUpdateEvent) {
            pipelineEventDispatcher.dispatch(
                PipelineUpdateEvent(
                    source = "update_pipeline",
                    projectId = setting.projectId,
                    pipelineId = setting.pipelineId,
                    version = settingVersion,
                    userId = userId
                )
            )
        }
        return setting.copy(version = settingVersion)
    }

    fun userGetSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode = ChannelCode.BS,
        version: Int = 0,
        checkPermission: Boolean = false,
        detailInfo: PipelineDetailInfo? = null,
        archiveFlag: Boolean? = false
    ): PipelineSetting {

        if (checkPermission) {
            val userPipelinePermissionCheckStrategy =
                UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
            UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        }
        return pipelineSettingVersionService.getPipelineSetting(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            detailInfo = detailInfo,
            channelCode = channelCode,
            version = version,
            archiveFlag = archiveFlag
        )
    }

    fun getCommonSetting(userId: String): PipelineCommonSetting {
        val inputComponentCommonSettings = mutableListOf<TaskComponentCommonSetting>()
        val inputTypeConfigMap = AtomUtils.getInputTypeConfigMap(taskCommonSettingConfig)
        inputTypeConfigMap.forEach { (componentType, maxSize) ->
            inputComponentCommonSettings.add(
                TaskComponentCommonSetting(
                    componentType = componentType,
                    maxSize = maxSize
                )
            )
        }
        val outputComponentCommonSettings = listOf(
            TaskComponentCommonSetting(
                componentType = KEY_DEFAULT,
                maxSize = taskCommonSettingConfig.maxDefaultOutputComponentSize
            )
        )
        val taskCommonSetting = TaskCommonSetting(
            maxInputNum = taskCommonSettingConfig.maxInputNum,
            maxOutputNum = taskCommonSettingConfig.maxOutputNum,
            inputComponentCommonSettings = inputComponentCommonSettings,
            outputComponentCommonSettings = outputComponentCommonSettings
        )
        return PipelineCommonSetting(
            maxStageNum = pipelineCommonSettingConfig.maxStageNum,
            stageCommonSetting = StageCommonSetting(
                maxJobNum = stageCommonSettingConfig.maxJobNum,
                jobCommonSetting = JobCommonSetting(
                    maxTaskNum = jobCommonSettingConfig.maxTaskNum,
                    taskCommonSetting = taskCommonSetting
                )
            )
        )
    }

    fun getSettingInfo(projectId: String, pipelineId: String): PipelineSetting? {
        return pipelineRepositoryService.getSetting(projectId, pipelineId)
    }

    private fun checkEditPermission(userId: String, projectId: String, pipelineId: String, message: String) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(message)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE
        ),
        content = ActionAuditContent.PIPELINE_EDIT_CONTENT
    )
    fun updatePipelineModel(
        userId: String,
        updatePipelineModelRequest: UpdatePipelineModelRequest,
        checkPermission: Boolean = true
    ): Boolean {
        val pipelineModelVersionList = updatePipelineModelRequest.pipelineModelVersionList
        if (checkPermission) {
            pipelineModelVersionList.forEach {
                checkEditPermission(
                    userId = it.creator,
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    message = "Need edit permission"
                )
            }
        }
        pipelineRepositoryService.batchUpdatePipelineModel(
            userId = userId,
            pipelineModelVersionList = pipelineModelVersionList
        )
        return true
    }

    fun rebuildSetting(
        oldSetting: PipelineSetting,
        projectId: String,
        newPipelineId: String,
        pipelineName: String
    ): PipelineSetting {
        return oldSetting.copy(
            projectId = projectId,
            pipelineId = newPipelineId,
            pipelineName = pipelineName,
            pipelineAsCodeSettings = PipelineAsCodeSettings()
        )
    }

    fun updateMaxConRunningQueueSize(
        userId: String,
        projectId: String,
        pipelineId: String,
        maxConRunningQueueSize: Int
    ): Int {
        return pipelineRepositoryService.updateMaxConRunningQueueSize(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            maxConRunningQueueSize = maxConRunningQueueSize
        )
    }
}
