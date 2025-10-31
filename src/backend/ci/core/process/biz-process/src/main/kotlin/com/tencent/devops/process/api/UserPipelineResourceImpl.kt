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

package com.tencent.devops.process.api

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditRequestBody
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.utils.MatrixYamlCheckUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineResource
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_LIST_LENGTH_LIMIT
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService.Companion.checkParam
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineRemoteToken
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.PipelineStageTag
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.BatchDeletePipeline
import com.tencent.devops.process.pojo.pipeline.PipelineCount
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.PipelineRecentUseService
import com.tencent.devops.process.service.PipelineRemoteAuthService
import com.tencent.devops.process.service.PipelineVersionFacadeService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import io.micrometer.core.annotation.Timed
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("LongParameterList")
class UserPipelineResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineRemoteAuthService: PipelineRemoteAuthService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val stageTagService: StageTagService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val auditService: AuditService,
    private val pipelineVersionFacadeService: PipelineVersionFacadeService,
    private val pipelineRuleService: PipelineRuleService,
    private val pipelineRecentUseService: PipelineRecentUseService
) : UserPipelineResource {

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(
            pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.CREATE
            )
        )
    }

    override fun pipelineExist(userId: String, projectId: String, pipelineName: String): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(
            data = pipelineInfoFacadeService.isPipelineExist(
                projectId = projectId, name = pipelineName, channelCode = ChannelCode.BS
            )
        )
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        permission: Permission,
        excludePipelineId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        checkParam(userId, projectId)
        val result = pipelineListFacadeService.hasPermissionList(
            userId = userId,
            projectId = projectId,
            permission = permission,
            excludePipelineId = excludePipelineId,
            filterByPipelineName = null,
            page = page,
            pageSize = pageSize
        )
        return Result(
            data = Page(
                page = page ?: 0,
                pageSize = pageSize ?: -1,
                count = result.count,
                records = result.records
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_CREATE)
    @Timed
    override fun create(
        userId: String,
        projectId: String,
        useTemplateSettings: Boolean?,
        pipeline: Model
    ): Result<PipelineId> {
        checkParam(userId, projectId)
        val pipelineId = PipelineId(
            id = pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = projectId,
                model = pipeline,
                channelCode = ChannelCode.BS,
                useSubscriptionSettings = useTemplateSettings
            ).pipelineId
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId.id,
                resourceName = pipeline.name,
                userId = userId,
                action = "create",
                actionContent = "Create",
                projectId = projectId
            )
        )
        return Result(pipelineId)
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: Permission
    ): Result<Boolean> {
        checkParam(userId, projectId)
        val bkAuthPermission = when (permission) {
            Permission.DEPLOY -> AuthPermission.DEPLOY
            Permission.DOWNLOAD -> AuthPermission.DOWNLOAD
            Permission.EDIT -> AuthPermission.EDIT
            Permission.EXECUTE -> AuthPermission.EXECUTE
            Permission.DELETE -> AuthPermission.DELETE
            Permission.VIEW -> AuthPermission.VIEW
            Permission.CREATE -> AuthPermission.CREATE
            Permission.LIST -> AuthPermission.LIST
        }

        return Result(
            pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = bkAuthPermission
            )
        )
    }

    @AuditEntry(
        actionId = ActionId.PIPELINE_CREATE,
        subActionIds = [ActionId.PIPELINE_EDIT]
    )
    override fun copy(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: PipelineCopy
    ): Result<PipelineId> {
        checkParam(userId, projectId)
        val pid = PipelineId(
            pipelineInfoFacadeService.copyPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineCopy = pipeline,
                channelCode = ChannelCode.BS
            )
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pid.id,
                resourceName = pipeline.name,
                userId = userId,
                action = "copy",
                actionContent = "Copy from($pipelineId)",
                projectId = projectId
            )
        )
        return Result(pid)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun editPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: Model
    ): Result<Boolean> {
        checkParam(userId, projectId)
        val pipelineResult = pipelineInfoFacadeService.editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = pipeline,
            yaml = null,
            versionStatus = VersionStatus.RELEASED,
            channelCode = ChannelCode.BS
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineResult.pipelineId,
                resourceName = pipeline.name,
                userId = userId,
                action = "edit",
                actionContent = "Edit Ver.${pipelineResult.version}",
                projectId = projectId
            )
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun saveAll(
        userId: String,
        projectId: String,
        pipelineId: String,
        modelAndSetting: PipelineModelAndSetting
    ): Result<Boolean> {
        checkParam(userId, projectId)
        modelAndSetting.setting.checkParam()
        modelAndSetting.setting.fixSubscriptions()
        val buildNumRule = modelAndSetting.setting.buildNumRule
        if (!buildNumRule.isNullOrBlank()) {
            pipelineRuleService.validateRuleStr(buildNumRule, PipelineRuleBusCodeEnum.BUILD_NUM.name)
        }
        val pipelineResult = pipelineInfoFacadeService.saveAll(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = modelAndSetting.model,
            setting = modelAndSetting.setting,
            channelCode = ChannelCode.BS
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = modelAndSetting.model.name,
                userId = userId,
                action = "edit",
                actionContent = "Save Ver.${pipelineResult.version}",
                projectId = projectId
            )
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun saveSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        @AuditRequestBody
        setting: PipelineSetting
    ): Result<Boolean> {
        checkParam(userId, projectId)
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = setting,
            checkPermission = true
        )
        pipelineInfoFacadeService.updatePipelineSettingVersion(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            operationLogType = OperationLogType.UPDATE_PIPELINE_SETTING,
            savedSetting = savedSetting
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = setting.pipelineName,
                userId = userId,
                action = "edit",
                actionContent = "Update Setting",
                projectId = projectId
            )
        )
        return Result(true)
    }

    override fun lockPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        enable: Boolean
    ): Result<Boolean> {
        checkParam(userId, projectId)

        pipelineInfoFacadeService.lockPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            enable = enable
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun rename(userId: String, projectId: String, pipelineId: String, name: PipelineName): Result<Boolean> {
        checkParam(userId, projectId)
        pipelineInfoFacadeService.renamePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            name = name.name,
            channelCode = ChannelCode.BS
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun get(
        userId: String,
        projectId: String,
        pipelineId: String,
        includeDraft: Boolean?
    ): Result<Model> {
        checkParam(userId, projectId)
        val pipeline = pipelineInfoFacadeService.getPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            includeDraft = includeDraft,
            channelCode = ChannelCode.BS
        )
        pipelineRecentUseService.record(userId, projectId, pipelineId)
        return Result(pipeline)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_VIEW)
    override fun getVersion(userId: String, projectId: String, pipelineId: String, version: Int): Result<Model> {
        checkParam(userId, projectId)
        return Result(
            pipelineInfoFacadeService.getPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS,
                version = version
            )
        )
    }

    override fun generateRemoteToken(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineRemoteToken> {
        checkParam(userId, projectId)
        val language = I18nUtil.getLanguage(userId)
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                language,
                arrayOf(
                    userId,
                    projectId,
                    AuthPermission.EDIT.getI18n(language),
                    pipelineId
                )
            )
        )
        return Result(
            pipelineRemoteAuthService.generateAuth(
                pipelineId = pipelineId,
                projectId = projectId,
                userId = userId
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_DELETE)
    override fun softDelete(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean?
    ): Result<Boolean> {
        checkParam(userId, projectId)
        val deletePipeline = pipelineInfoFacadeService.deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS,
            archiveFlag = archiveFlag
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = deletePipeline.pipelineName,
                userId = userId,
                action = "delete",
                actionContent = "Delete Pipeline",
                projectId = projectId
            )
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_DELETE)
    override fun batchDelete(
        userId: String,
        batchDeletePipeline: BatchDeletePipeline,
        archiveFlag: Boolean?
    ): Result<Map<String, Boolean>> {
        val pipelineIds = batchDeletePipeline.pipelineIds
        if (pipelineIds.isEmpty()) {
            return Result(emptyMap())
        }
        if (pipelineIds.size > 100) {
            throw InvalidParamException(
                I18nUtil.getCodeLanMessage(PIPELINE_LIST_LENGTH_LIMIT)
            )
        }
        val result = pipelineIds.associateWith {
            try {
                softDelete(
                    userId = userId,
                    projectId = batchDeletePipeline.projectId,
                    pipelineId = it,
                    archiveFlag = archiveFlag
                ).data ?: false
            } catch (ignore: Exception) {
                false
            }
        }
        return Result(result)
    }

    @AuditEntry(actionId = ActionId.PIPELINE_DELETE)
    override fun deleteVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<Boolean> {
        checkParam(userId, projectId)
        val pipelineName = pipelineVersionFacadeService.deletePipelineVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = pipelineName,
                userId = userId,
                action = "delete",
                actionContent = "Delete Ver.$version",
                projectId = projectId
            )
        )
        return Result(true)
    }

    override fun getCount(userId: String, projectId: String): Result<PipelineCount> {
        checkParam(userId, projectId)
        return Result(pipelineListFacadeService.getCount(userId, projectId))
    }

    @AuditEntry(actionId = ActionId.PROJECT_MANAGE)
    override fun restore(userId: String, projectId: String, pipelineId: String): Result<Boolean> {
        checkParam(userId, projectId)
        val restorePipeline = pipelineInfoFacadeService.restorePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = restorePipeline.pipelineName,
                userId = userId,
                action = "Restore",
                actionContent = "Restore Ver.${restorePipeline.version}",
                projectId = projectId
            )
        )
        return Result(true)
    }

    override fun recycleList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        collation: PipelineCollation?,
        filterByPipelineName: String?
    ): Result<PipelineViewPipelinePage<PipelineInfo>> {
        checkParam(userId, projectId)
        return Result(
            pipelineListFacadeService.listDeletePipelineIdByProject(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = sortType ?: PipelineSortType.CREATE_TIME, ChannelCode.BS,
                collation = collation ?: PipelineCollation.DEFAULT,
                filterByPipelineName = filterByPipelineName
            )
        )
    }

    override fun listViewSettingAndPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<PipelineViewAndPipelines> {
        checkParam(userId, projectId)
        return Result(pipelineListFacadeService.listViewAndPipelines(userId, projectId, page, pageSize))
    }

    @Timed
    override fun listViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        filterByViewIds: String?,
        viewId: String,
        collation: PipelineCollation?,
        showDelete: Boolean?
    ): Result<PipelineViewPipelinePage<Pipeline>> {
        checkParam(userId, projectId)
        return Result(
            pipelineListFacadeService.listViewPipelines(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = sortType ?: PipelineSortType.CREATE_TIME,
                channelCode = ChannelCode.BS,
                viewId = viewId,
                checkPermission = true,
                filterByPipelineName = filterByPipelineName,
                filterByCreator = filterByCreator,
                filterByLabels = filterByLabels,
                filterByViewIds = filterByViewIds,
                collation = collation ?: PipelineCollation.DEFAULT,
                showDelete = showDelete ?: false,
                queryByWeb = true
            )
        )
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        filterByPipelineName: String?
    ): Result<PipelinePage<Pipeline>> {
        checkParam(userId, projectId)
        return Result(
            pipelineListFacadeService.listPermissionPipeline(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                sortType = sortType ?: PipelineSortType.CREATE_TIME, ChannelCode.BS,
                checkPermission = true,
                filterByPipelineName = filterByPipelineName
            )
        )
    }

    override fun getPipelineStatus(
        userId: String,
        projectId: String,
        pipelines: Set<String>
    ): Result<Map<String, PipelineStatus>> {
        checkParam(userId, projectId)
        val status = pipelineListFacadeService.getPipelineStatus(userId, projectId, pipelines)
        val currentTimestamp = System.currentTimeMillis()
        return Result(
            status.associate {
                it.pipelineId to PipelineStatus(
                    taskCount = it.taskCount,
                    buildCount = it.buildCount,
                    lock = it.lock,
                    canManualStartup = it.canManualStartup,
                    latestBuildStartTime = it.latestBuildStartTime,
                    latestBuildEndTime = it.latestBuildEndTime,
                    latestBuildStatus = it.latestBuildStatus,
                    latestBuildNum = it.latestBuildNum,
                    latestBuildTaskName = it.latestBuildTaskName,
                    latestBuildEstimatedExecutionSeconds = it.latestBuildEstimatedExecutionSeconds,
                    latestBuildId = it.latestBuildId,
                    currentTimestamp = currentTimestamp,
                    runningBuildCount = it.runningBuildCount,
                    hasCollect = it.hasCollect
                )
            }
        )
    }

    override fun getStageTag(userId: String): Result<List<PipelineStageTag>> {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        return stageTagService.getAllStageTag()
    }

    override fun favor(userId: String, projectId: String, pipelineId: String, favor: Boolean): Result<Boolean> {
        return Result(pipelineGroupService.favorPipeline(userId, projectId, pipelineId, favor))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun exportPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean?
    ): Response {
        return pipelineInfoFacadeService.exportPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            archiveFlag = archiveFlag
        )
    }

    @AuditEntry(
        actionId = ActionId.PIPELINE_CREATE,
        subActionIds = [ActionId.PIPELINE_EDIT]
    )
    override fun uploadPipeline(
        userId: String,
        @AuditRequestBody
        pipelineInfo: PipelineModelAndSetting,
        projectId: String
    ): Result<String?> {

        val pipelineId = pipelineInfoFacadeService.uploadPipeline(
            userId = userId,
            projectId = projectId,
            pipelineModelAndSetting = pipelineInfo
        )

        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = pipelineInfo.setting.pipelineName,
                userId = userId,
                action = "create",
                actionContent = "Import Create",
                projectId = projectId
            )
        )
        return Result(pipelineId)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    override fun versionList(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineVersionWithInfo>> {
        checkParam(userId, projectId)
        return Result(
            pipelineVersionFacadeService.listPipelineVersionInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                fromVersion = null,
                versionName = null,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

    override fun checkYaml(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: MatrixPipelineInfo
    ): Result<MatrixPipelineInfo> {
        checkParam(userId, projectId)
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        return Result(MatrixYamlCheckUtils.checkYaml(yaml))
    }

    override fun countInheritedDialectPipeline(
        userId: String,
        projectId: String
    ): Result<Long> {
        return Result(
            pipelineListFacadeService.countInheritedDialectPipeline(projectId = projectId)
        )
    }

    override fun listInheritedDialectPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineIdAndName>> {
        return Result(
            pipelineListFacadeService.listInheritedDialectPipelines(
                projectId = projectId,
                page = page,
                pageSize = pageSize
            )
        )
    }
}
