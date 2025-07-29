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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.BuildNoUpdateReq
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineVersionResource
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.PipelineRecentUseService
import com.tencent.devops.process.service.PipelineVersionFacadeService
import com.tencent.devops.process.strategy.context.UserPipelinePermissionCheckContext
import com.tencent.devops.process.strategy.factory.UserPipelinePermissionCheckStrategyFactory
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserPipelineVersionResourceImpl @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val auditService: AuditService,
    private val pipelineVersionFacadeService: PipelineVersionFacadeService,
    private val pipelineOperationLogService: PipelineOperationLogService,
    private val pipelineRecentUseService: PipelineRecentUseService
) : UserPipelineVersionResource {

    override fun getPipelineVersionDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean?
    ): Result<PipelineDetail> {
        checkParam(userId, projectId)
        if (archiveFlag != true) {
            pipelineRecentUseService.record(userId, projectId, pipelineId)
        }
        return Result(
            pipelineVersionFacadeService.getPipelineDetailIncludeDraft(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun preFetchDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        targetAction: CodeTargetAction?,
        repoHashId: String?,
        targetBranch: String?
    ): Result<PrefetchReleaseResult> {
        checkParam(userId, projectId)
        val permission = AuthPermission.EDIT
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(
            pipelineVersionFacadeService.preFetchDraftVersion(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                targetAction = targetAction,
                repoHashId = repoHashId,
                targetBranch = targetBranch
            )
        )
    }

    override fun releaseDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        request: PipelineVersionReleaseRequest
    ): Result<DeployPipelineResult> {
        checkParam(userId, projectId)
        val permission = AuthPermission.EDIT
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(
            pipelineVersionFacadeService.releaseDraftVersion(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                request = request
            )
        )
    }

    override fun createPipelineFromTemplate(
        userId: String,
        projectId: String,
        request: TemplateInstanceCreateRequest
    ): Result<DeployPipelineResult> {
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            AuthPermission.CREATE
        )
        return Result(
            pipelineVersionFacadeService.createPipelineFromFreedom(
                userId = userId,
                projectId = projectId,
                request = request
            )
        )
    }

    override fun getVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        archiveFlag: Boolean?
    ): Result<PipelineVersionWithModel> {
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        return Result(
            pipelineVersionFacadeService.getVersion(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun previewCode(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<PreviewResponse> {
        val permission = AuthPermission.VIEW
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(
            pipelineVersionFacadeService.preview(userId, projectId, pipelineId, version)
        )
    }

    override fun savePipelineDraft(
        userId: String,
        projectId: String,
        modelAndYaml: PipelineVersionWithModelRequest
    ): Result<DeployPipelineResult> {
        checkParam(userId, projectId)
        val result = pipelineVersionFacadeService.savePipelineDraft(
            userId = userId,
            projectId = projectId,
            modelAndYaml = modelAndYaml
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = result.pipelineId,
                resourceName = result.pipelineName,
                userId = userId,
                action = "edit",
                actionContent = "Save Ver.${result.version}",
                projectId = projectId
            )
        )
        return Result(result)
    }

    override fun versionCreatorList(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<String>> {
        checkParam(userId, projectId)
        val permission = AuthPermission.VIEW
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        val result = pipelineVersionFacadeService.getVersionCreatorInPage(
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize
        )
        return Result(result)
    }

    override fun versionList(
        userId: String,
        projectId: String,
        pipelineId: String,
        fromVersion: Int?,
        versionName: String?,
        includeDraft: Boolean?,
        creator: String?,
        description: String?,
        buildOnly: Boolean?,
        page: Int?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<Page<PipelineVersionSimple>> {
        checkParam(userId, projectId)
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        return Result(
            pipelineVersionFacadeService.listPipelineVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                fromVersion = fromVersion,
                includeDraft = includeDraft,
                versionName = versionName?.takeIf { it.isNotBlank() },
                creator = creator?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                page = page ?: 1,
                pageSize = pageSize ?: 5,
                buildOnly = buildOnly,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun getVersionInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        archiveFlag: Boolean?
    ): Result<PipelineVersionSimple> {
        checkParam(userId, projectId)
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        return Result(
            pipelineVersionFacadeService.getPipelineVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun getPipelineOperationLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        creator: String?,
        page: Int?,
        pageSize: Int?,
        archiveFlag: Boolean?
    ): Result<Page<PipelineOperationDetail>> {
        checkParam(userId, projectId)
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        return Result(
            pipelineOperationLogService.getOperationLogsInPage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                creator = creator,
                page = page,
                pageSize = pageSize,
                archiveFlag = archiveFlag
            )
        )
    }

    override fun operatorList(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<List<String>> {
        checkParam(userId, projectId)
        val permission = AuthPermission.VIEW
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        val result = pipelineOperationLogService.getOperatorInPage(
            projectId = projectId,
            pipelineId = pipelineId
        )
        return Result(result)
    }

    override fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineVersionSimple> {
        checkParam(userId, projectId)
        val permission = AuthPermission.EDIT
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(
            pipelineVersionFacadeService.rollbackDraftFromVersion(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
        )
    }

    override fun exportPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?,
        storageType: String?
    ): Response {
        return pipelineInfoFacadeService.exportPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            storageType = PipelineStorageType.getActionType(storageType)
        )
    }

    override fun updateBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: BuildNoUpdateReq
    ): Result<Boolean> {
        pipelineInfoFacadeService.updateBuildNo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            targetBuildNo = buildNo.currentBuildNo
        )
        return Result(true)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}
