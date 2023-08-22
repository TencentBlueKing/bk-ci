/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.PipelineModelAndYaml
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineVersionResource
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.engine.pojo.PipelineResVersion
import com.tencent.devops.process.engine.service.PipelineVersionFacadeService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.transfer.PreviewResponse
import com.tencent.devops.process.pojo.transfer.TransferActionType
import com.tencent.devops.process.pojo.transfer.TransferBody
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.transfer.PipelineTransferYamlService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserPipelineVersionResourceImpl @Autowired constructor(
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val auditService: AuditService,
    private val pipelineVersionFacadeService: PipelineVersionFacadeService,
    private val pipelineOperationLogService: PipelineOperationLogService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val templateFacadeService: TemplateFacadeService
) : UserPipelineVersionResource {

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
        val templateDetail = templateFacadeService.getTemplate(
            userId = userId,
            projectId = projectId,
            templateId = request.templateId,
            version = request.templateVersion
        )
        return Result(
            pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = projectId,
                model = templateDetail.template.copy(templateId = request.templateId),
                channelCode = ChannelCode.BS,
                checkPermission = false,
                instanceType = request.instanceType,
                saveDraft = true,
                useSubscriptionSettings = request.useSubscriptionSettings,
                useLabelSettings = request.useLabelSettings,
                useConcurrencyGroup = request.useConcurrencyGroup
            )
        )
    }

    override fun getVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineModelAndYaml> {
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
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val setting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = resource.settingVersion ?: version
        )
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = resource.model
        )
        val yaml = resource.yaml ?: transferService.transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(modelAndSetting)
        ).newYaml
        return Result(
            PipelineModelAndYaml(
                modelAndSetting = modelAndSetting,
                yaml = yaml,
                description = resource.description,
                baseVersion = resource.version,
                baseVersionName = resource.versionName
            )
        )
    }

    override fun preview(
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
            transferService.buildPreview(userId, projectId, pipelineId, version)
        )
    }

    override fun savePipelineDraft(
        userId: String,
        projectId: String,
        pipelineId: String,
        modelAndYaml: PipelineModelAndYaml
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
        val baseVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = modelAndYaml.baseVersion,
            includeDraft = true
        )
        val transferResult = transferService.transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            actionType = TransferActionType.FULL_YAML2MODEL,
            data = TransferBody(
                modelAndSetting = modelAndYaml.modelAndSetting,
                oldYaml = baseVersion?.yaml ?: ""
            )
        )
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            setting = transferResult.modelAndSetting?.setting ?: modelAndYaml.modelAndSetting.setting,
            checkPermission = false,
            dispatchPipelineUpdateEvent = false,
            saveDraft = true
        )
        val pipelineResult = pipelineInfoFacadeService.editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = transferResult.modelAndSetting?.model ?: modelAndYaml.modelAndSetting.model,
            channelCode = ChannelCode.BS,
            checkPermission = false,
            checkTemplate = false,
            saveDraft = true,
            description = modelAndYaml.description,
            yaml = transferResult.newYaml,
            savedSetting = savedSetting
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = modelAndYaml.modelAndSetting.model.name,
                userId = userId,
                action = "edit",
                actionContent = "Save Ver.${pipelineResult.version}",
                projectId = projectId
            )
        )
        return Result(pipelineResult)
    }

    override fun creatorList(
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
        creator: String?,
        description: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineResVersion>> {
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
        return Result(
            pipelineVersionFacadeService.listPipelineVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                creator = creator?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getPipelineOperationLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<PipelineOperationDetail>> {
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
        return Result(
            pipelineOperationLogService.getOperationLogsInPage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize
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
    ): Result<Boolean> {
        TODO("Not yet implemented")
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
