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

package com.tencent.devops.process.api.template.v2

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.PipelineTemplateVersionSimple
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.PipelineTemplateListResponse
import com.tencent.devops.process.pojo.template.PipelineTemplateListSimpleResponse
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.v2.PTemplateModelTransferResult
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineRefInfo
import com.tencent.devops.process.pojo.template.v2.PTemplateSource2Count
import com.tencent.devops.process.pojo.template.v2.PTemplateTransferBody
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCompareResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCopyCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCustomCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDetailsResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftReleaseReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftSaveReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateStrategyUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PreFetchTemplateReleaseResult
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.template.v2.PipelineTemplateFacadeService
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory

@RestResource
class UserPipelineTemplateV2ResourceImpl(
    private val permissionService: PipelineTemplatePermissionService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val templateFacadeService: PipelineTemplateFacadeService,
    private val pipelineOperationLogService: PipelineOperationLogService
) : UserPipelineTemplateV2Resource {
    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_CREATE)
    override fun create(
        userId: String,
        projectId: String,
        request: PipelineTemplateCustomCreateReq
    ): Result<DeployTemplateResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        return Result(templateFacadeService.create(userId = userId, projectId = projectId, request = request))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_CREATE)
    override fun createByMarket(
        userId: String,
        projectId: String,
        templateId: String?,
        request: PipelineTemplateMarketCreateReq
    ): Result<DeployTemplateResult> {
        return Result(
            templateFacadeService.createByMarket(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                request = request
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_CREATE)
    override fun copy(
        userId: String,
        projectId: String,
        request: PipelineTemplateCopyCreateReq
    ): Result<DeployTemplateResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = request.srcTemplateId
        )
        return Result(
            templateFacadeService.copy(
                userId = userId,
                projectId = projectId,
                request = request
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_DELETE)
    override fun delete(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<Boolean> {
        logger.info("delete template {}|{}|{}", userId, projectId, templateId)
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.DELETE,
            templateId = templateId
        )
        return Result(
            templateFacadeService.deleteTemplate(
                userId = userId,
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun saveDraft(
        userId: String,
        projectId: String,
        templateId: String?,
        request: PipelineTemplateDraftSaveReq
    ): Result<DeployTemplateResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.saveDraft(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                request = request
            )
        )
    }

    override fun listTemplateInfos(
        userId: String,
        projectId: String,
        request: PipelineTemplateCommonCondition
    ): Result<SQLPage<PipelineTemplateListResponse>> {
        return Result(templateFacadeService.listTemplateInfos(userId, request))
    }

    override fun listTemplateSimpleInfos(
        userId: String,
        projectId: String,
        request: PipelineTemplateCommonCondition
    ): Result<SQLPage<PipelineTemplateListSimpleResponse>> {
        return Result(templateFacadeService.listTemplateSimpleInfos(userId, request))
    }

    override fun listAllTemplates(
        userId: String,
        projectId: String
    ): Result<OptionalTemplateList> {
        return Result(templateFacadeService.listAllTemplates(userId, projectId))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun getTemplateDetails(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<PipelineTemplateDetailsResponse> {
        logger.info("get template details {}|{}|{}|{}", userId, projectId, templateId, version)
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.getTemplateDetails(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun getLatestTemplateDetails(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<PipelineTemplateDetailsResponse> {
        logger.info("get latest template details {}|{}|{}", userId, projectId, templateId)
        return Result(
            templateFacadeService.getTemplateDetails(
                projectId = projectId,
                templateId = templateId,
                version = null
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun getRefTemplateDetails(
        userId: String,
        projectId: String,
        templateId: String,
        ref: String
    ): Result<PipelineTemplateDetailsResponse> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.getRefTemplateDetails(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                ref = ref
            )
        )
    }

    override fun getPipelineRelatedTemplateDetails(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineTemplateDetailsResponse?> {
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
            templateFacadeService.getPipelineRelatedTemplateDetails(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = version
            )
        )
    }

    override fun getPipelineRelatedTemplateInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PTemplatePipelineRefInfo?> {
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
            templateFacadeService.getPipelineRelatedTemplateInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = version
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun getTemplateInfo(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<PipelineTemplateInfoResponse> {
        logger.info("get template info {}|{}|{}", userId, projectId, templateId)
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.getTemplateInfo(
                userId = userId,
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    override fun getType2Count(userId: String, projectId: String): Result<Map<String, Int>> {
        return Result(templateFacadeService.getType2Count(userId, projectId))
    }

    override fun getSource2Count(
        userId: String,
        projectId: String,
        commonCondition: PipelineTemplateCommonCondition
    ): Result<PTemplateSource2Count> {
        return Result(templateFacadeService.getSource2Count(userId, projectId, commonCondition))
    }

    override fun getTemplateVersions(
        userId: String,
        projectId: String,
        templateId: String,
        request: PipelineTemplateResourceCommonCondition
    ): Result<Page<PipelineTemplateVersionSimple>> {
        logger.info("get template versions {}|{}|{}|{}", userId, projectId, templateId, request)
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.getTemplateVersions(
                projectId = projectId,
                templateId = templateId,
                commonCondition = request
            )
        )
    }

    override fun compare(
        userId: String,
        projectId: String,
        templateId: String,
        baseVersion: Long,
        comparedVersion: Long
    ): Result<PipelineTemplateCompareResponse> {
        logger.info("compare template {}|{}|{}|{}|{}", userId, projectId, templateId, baseVersion, comparedVersion)
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.compare(
                projectId = projectId,
                templateId = templateId,
                baseVersion = baseVersion,
                comparedVersion = comparedVersion
            )
        )
    }

    override fun preFetchDraftVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        customVersionName: String?,
        enablePac: Boolean,
        targetAction: CodeTargetAction?,
        repoHashId: String?,
        targetBranch: String?
    ): Result<PreFetchTemplateReleaseResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.preFetchDraftVersion(
                projectId = projectId,
                templateId = templateId,
                version = version,
                customVersionName = customVersionName,
                enablePac = enablePac,
                repoHashId = repoHashId,
                targetAction = targetAction,
                targetBranch = targetBranch
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun releaseDraftVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateDraftReleaseReq
    ): Result<DeployTemplateResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.releaseDraft(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                request = request
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun getPipelineOperationLogs(
        userId: String,
        projectId: String,
        templateId: String,
        creator: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineOperationDetail>> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.getOperationLogsInPage(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                creator = creator,
                page = page,
                pageSize = pageSize
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_VIEW)
    override fun operatorList(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<List<String>> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            pipelineOperationLogService.getOperatorInPage(
                projectId = projectId,
                pipelineId = templateId
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<DeployTemplateResult> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.rollbackDraft(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_DELETE)
    override fun deleteVersion(userId: String, projectId: String, templateId: String, version: Long): Result<Boolean> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.DELETE,
            templateId = templateId
        )
        templateFacadeService.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        return Result(true)
    }

    override fun hasPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String?,
        permission: AuthPermission
    ): Result<Boolean> {
        return Result(
            permissionService.checkPipelineTemplatePermission(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                permission = permission
            )
        )
    }

    override fun enableTemplatePermissionManage(
        userId: String,
        projectId: String
    ): Result<Boolean> {
        return Result(permissionService.enableTemplatePermissionManage(projectId))
    }

    override fun transfer(
        userId: String,
        projectId: String,
        storageType: PipelineStorageType,
        body: PTemplateTransferBody
    ): Result<PTemplateModelTransferResult> {
        return Result(
            templateFacadeService.transfer(
                userId = userId,
                projectId = projectId,
                storageType = storageType,
                body = body
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun exportTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Response {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return templateFacadeService.exportTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun transformTemplateToCustom(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<Boolean> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.transformTemplateToCustom(
                userId = userId,
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun updateUpgradeStrategy(
        userId: String,
        projectId: String,
        templateId: String,
        request: PipelineTemplateStrategyUpdateInfo
    ): Result<Boolean> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT,
            templateId = templateId
        )
        return Result(
            templateFacadeService.updateUpgradeStrategy(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                request = request
            )
        )
    }

    override fun previewTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        highlightType: HighlightType?
    ): Result<TemplatePreviewDetail> {
        permissionService.checkPipelineTemplatePermissionWithMessage(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW,
            templateId = templateId
        )
        return Result(
            templateFacadeService.previewTemplate(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                highlightType = highlightType
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserPipelineTemplateV2ResourceImpl::class.java)
    }
}
