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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
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
import com.tencent.devops.process.service.pipeline.version.PipelineRunEnvOsChangeResolver
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
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRunEnvOsChangeResolver: PipelineRunEnvOsChangeResolver,
    private val client: Client
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
        updateVersion: Boolean = true,
        // 本次与设置一并保存的编排，为空表示本次不改编排、校验以已落库的编排为准。
        savingModel: Model? = null
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
        checkRunEnvOsCompatibility(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = setting,
            savingModel = savingModel
        )
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
                    resourceType = AuthResourceType.getAuthResourceTypeByChannel(AuthResourceType.PIPELINE_DEFAULT).value,
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

    /**
     * 校验编排中的插件是否都适用于本次设置所指定的运行环境操作系统。
     *
     * 保存设置本身不经过编排校验，但创作流的运行环境正是由设置里的 envHashId 指定的，换一个环境就等于
     * 把编排里的插件挪到了另一种操作系统上。若只在编排保存入口校验，改设置就会持久化出
     * 「运行环境与编排中插件不适配」的状态，而构建期只校验插件的服务范围与构建环境类型、不校验操作系统，
     * 该不一致会一直漏到运行时才失败。
     *
     * 普通流水线的运行环境写在编排的 Job 上，设置里没有可改的运行环境，因此改设置改不动任何插件的运行系统，
     * 在第一个判断处即返回，不产生任何查询；其校验由编排保存入口按 Job 逐个完成。
     */
    private fun checkRunEnvOsCompatibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        setting: PipelineSetting,
        savingModel: Model?
    ) {
        // envHashId 是「运行环境由设置指定」这类渠道特有的字段，为空即本次保存没有设置层面的运行环境。
        // 该判断置于最前，普通流水线保存设置不会走到后面任何一次查询，零额外开销
        if (setting.envHashId.isNullOrBlank()) return
        // 渠道以流水线自身记录为准，不能取请求上下文：openapi 的请求渠道由网关部署标签决定
        // (见 ApiGatewayUtil.getChannelCode)，与流水线实际所属渠道无关，取错会让校验静默失效
        val channelCode = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        )?.channelCode ?: return
        // 先判渠道再解析环境，避免为运行环境不由设置指定的渠道白查一次
        if (!pipelineRunEnvOsChangeResolver.isRunEnvSpecifiedBySetting(channelCode)) return
        val runEnvOsChange = pipelineRunEnvOsChangeResolver.resolve(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            setting = setting
        ) ?: return
        // 本次没有把插件挪到另一种操作系统上时，先写设置不会产生「环境已改、编排未改」的不一致，
        // 编排里的插件是否适配交由编排保存入口校验即可。重命名、改通知配置等绝大多数保存都走这一分支，
        // 在此提前返回可省掉一次编排查询与一次插件批量查询。
        // 注意不能用 previousOs == null 判断：它同时表示「首次为流水线指定运行环境」，那属于需要校验的变更
        if (runEnvOsChange.previousOs == runEnvOsChange.currentOs) return
        // 保存设置的对象必然是已存在的流水线，故此处必有校验入参
        val runEnvOsCheckParam = pipelineRepositoryService.buildRunEnvOsCheckParam(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            runEnvOsChange = runEnvOsChange
        ) ?: return
        // 本次一并保存编排时以新编排为准，否则取已落库的编排(草稿优先：草稿才是用户当前正在编辑、后续会发布的那份)
        val model = savingModel ?: pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            includeDraft = true
        )?.model ?: return
        AtomUtils.checkModelRunEnvOs(
            projectCode = projectId,
            model = model,
            runEnvOsCheckParam = runEnvOsCheckParam,
            client = client
        )
    }

    fun userGetSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode = ChannelCode.getRequestChannelCode(),
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

    fun getDefaultSetting(userId: String): PipelineSetting {
        return pipelineRepositoryService.createDefaultSetting(channelCode = ChannelCode.getRequestChannelCode())
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

    fun getPipelineSettingByDraftVersion(
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int
    ): PipelineSetting {
        return pipelineSettingVersionService.getPipelineSettingByDraftVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            draftVersion = draftVersion
        )
    }
}
