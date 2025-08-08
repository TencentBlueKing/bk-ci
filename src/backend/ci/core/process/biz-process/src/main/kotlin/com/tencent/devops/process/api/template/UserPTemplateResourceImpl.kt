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

package com.tencent.devops.process.api.template

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.PTemplateOrderByType
import com.tencent.devops.process.pojo.PTemplateSortType
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.HighlightType
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateId
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplatePreviewDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.template.TemplateCommonService
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.service.template.TemplatePACService
import com.tencent.devops.process.service.template.TemplateSettingService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-01-08
 */
@RestResource
class UserPTemplateResourceImpl @Autowired constructor(
    private val templateFacadeService: TemplateFacadeService,
    private val templatePACService: TemplatePACService,
    private val templateSettingService: TemplateSettingService,
    private val templateCommonService: TemplateCommonService
) : UserPTemplateResource {

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_CREATE)
    override fun createTemplate(userId: String, projectId: String, template: Model): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.createTemplate(projectId, userId, template)))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_DELETE)
    override fun deleteTemplate(userId: String, projectId: String, templateId: String): Result<Boolean> {
        return Result(templateFacadeService.deleteTemplate(projectId, userId, templateId))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_DELETE)
    override fun deleteTemplate(userId: String, projectId: String, templateId: String, version: Long): Result<Boolean> {
        return Result(
            templateFacadeService.deleteTemplate(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_DELETE)
    override fun deleteTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String
    ): Result<Boolean> {
        return Result(
            templateFacadeService.deleteTemplate(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                versionName = versionName
            )
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Result<Boolean> {
        return Result(templateFacadeService.updateTemplate(projectId, userId, templateId, versionName, template) > 0)
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        orderBy: PTemplateOrderByType?,
        sort: PTemplateSortType?,
        page: Int?,
        pageSize: Int?
    ): Result<TemplateListModel> {
        return Result(
            templateFacadeService.listTemplate(
                projectId = projectId,
                userId = userId,
                templateType = templateType,
                storeFlag = storeFlag,
                orderBy = orderBy,
                sort = sort,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listAllTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        page: Int?,
        pageSize: Int?
    ): Result<OptionalTemplateList> {
        return Result(templateFacadeService.listAllTemplate(userId, projectId, templateType, null, page, pageSize))
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<TemplateModelDetail> {
        return Result(templateFacadeService.getTemplate(projectId, userId, templateId, version))
    }

    @Suppress("ALL")
    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun updateTemplateSetting(
        userId: String,
        projectId: String,
        templateId: String,
        setting: PipelineSetting
    ): Result<Boolean> {
        if (setting.runLockType == PipelineRunLockType.SINGLE ||
            setting.runLockType == PipelineRunLockType.SINGLE_LOCK ||
            setting.runLockType == PipelineRunLockType.GROUP_LOCK ||
            setting.runLockType == PipelineRunLockType.MULTIPLE
        ) {
            if (setting.waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN ||
                setting.waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
            ) {
                throw InvalidParamException(I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_QUEUE_LENGTH_ILLEGAL))
            }
            if (setting.maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                setting.maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
            ) {
                throw InvalidParamException(
                    I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_NUMBER_QUEUES_ILLEGAL)
                )
            }
        }
        return Result(templateSettingService.updateTemplateSetting(projectId, userId, templateId, setting))
    }

    override fun getTemplateSetting(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<PipelineSetting> {
        return Result(templateSettingService.getTemplateSetting(projectId, userId, templateId))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun copyTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        copyTemplateReq: CopyTemplateReq
    ): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.copyTemplate(userId, projectId, templateId, copyTemplateReq)))
    }

    @AuditEntry(actionId = ActionId.PIPELINE_TEMPLATE_EDIT)
    override fun saveAsTemplate(
        userId: String,
        projectId: String,
        saveAsTemplateReq: SaveAsTemplateReq
    ): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.saveAsTemplate(userId, projectId, saveAsTemplateReq)))
    }

    override fun hasManagerPermission(userId: String, projectId: String): Result<Boolean> {
        return Result(templateCommonService.hasManagerPermission(projectId, userId))
    }

    override fun previewTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        highlightType: HighlightType?
    ): Result<TemplatePreviewDetail> {
        return Result(templatePACService.previewTemplate(userId, projectId, templateId, highlightType))
    }

    override fun hasPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String?,
        permission: AuthPermission
    ): Result<Boolean> {
        return Result(
            templateFacadeService.hasPipelineTemplatePermission(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                permission = permission
            )
        )
    }

    override fun enableTemplatePermissionManage(userId: String, projectId: String): Result<Boolean> {
        return Result(templateFacadeService.enableTemplatePermissionManage(projectId))
    }
}
